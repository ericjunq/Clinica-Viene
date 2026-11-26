package br.com.clinicaviene.identidade.service;

import br.com.clinicaviene.auditoria.dto.RegistroAuditoria;
import br.com.clinicaviene.auditoria.enums.AcaoAuditoria;
import br.com.clinicaviene.auditoria.service.AuditoriaService;
import br.com.clinicaviene.compartilhado.cripto.CpfHasher;
import br.com.clinicaviene.compartilhado.exceptions.RegraNegocioException;
import br.com.clinicaviene.compartilhado.util.CpfUtils;
import br.com.clinicaviene.identidade.auth.PerfilPermissoes;
import br.com.clinicaviene.identidade.auth.UsuarioJwtConverter;
import br.com.clinicaviene.identidade.dto.NovoUsuarioDto;
import br.com.clinicaviene.identidade.dto.UsuarioAutenticado;
import br.com.clinicaviene.identidade.entity.Usuario;
import br.com.clinicaviene.identidade.enums.Perfil;
import br.com.clinicaviene.identidade.enums.StatusUsuario;
import br.com.clinicaviene.identidade.exceptions.UsuarioNaoEncontradoException;
import br.com.clinicaviene.identidade.repository.UsuarioRepository;
import br.com.clinicaviene.identidade.validation.PoliticaSenhaValidator;
import br.com.clinicaviene.identidade.validation.UfValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de usuários internos. A autorização é feita aqui (camada de serviço), não só no controller: cada
 * método recebe quem está agindo ({@link UsuarioAutenticado}) e aplica {@link PerfilPermissoes}.
 */
@Service
public class UsuarioService {

    private static final String ENTIDADE = "Usuario";

    private final UsuarioRepository repositorio;
    private final CpfHasher hashCpf;
    private final PasswordEncoder codificador;
    private final AuditoriaService auditoria;
    private final SessaoService sessoes;
    private final Clock relogio;

    public UsuarioService(
            UsuarioRepository repositorio,
            CpfHasher hashCpf,
            PasswordEncoder codificador,
            AuditoriaService auditoria,
            SessaoService sessoes,
            Clock relogio) {
        this.repositorio = repositorio;
        this.hashCpf = hashCpf;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.sessoes = sessoes;
        this.relogio = relogio;
    }

    @Transactional
    public Usuario criar(NovoUsuarioDto dados, UsuarioAutenticado ator) {
        if (!PerfilPermissoes.gerenciaUsuarios(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para criar usuários");
        }
        if (dados.perfil() == Perfil.DEV) {
            throw new RegraNegocioException("Usuários DEV não podem ser criados pelo sistema.");
        }
        if (!PerfilPermissoes.podeCriar(ator.perfil(), dados.perfil())) {
            throw new AccessDeniedException("Sem permissão para criar este perfil");
        }
        Usuario usuario = salvarNovo(normalizar(dados), ator.id());
        auditoria.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.USUARIO_CRIADO, ENTIDADE, usuario.getId()));
        return usuario;
    }

    /** Usado só pelo inicializador, com o banco sem usuários (D-048). */
    @Transactional
    public Usuario criarDevInicial(NovoUsuarioDto dados) {
        if (repositorio.count() > 0) {
            throw new IllegalStateException("O usuário DEV inicial só pode ser criado com o banco sem usuários");
        }
        Usuario usuario = salvarNovo(normalizar(dados), null);
        auditoria.registrar(
                RegistroAuditoria.sobre(null, AcaoAuditoria.USUARIO_INICIAL_CRIADO, ENTIDADE, usuario.getId()));
        return usuario;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar(UsuarioAutenticado ator) {
        return switch (ator.perfil()) {
            case DEV -> repositorio.findAllByOrderByNomeCompletoAsc();
            case MASTER -> repositorio.findAllByPerfilNotOrderByNomeCompletoAsc(Perfil.DEV);
            case ADMIN, PROFISSIONAL -> throw new AccessDeniedException("Sem permissão para listar usuários");
        };
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID id, UsuarioAutenticado ator) {
        if (!PerfilPermissoes.gerenciaUsuarios(ator.perfil()) && !ator.id().equals(id)) {
            throw new AccessDeniedException("Sem permissão para ver usuários");
        }
        Usuario usuario = repositorio.findById(id).orElseThrow(UsuarioNaoEncontradoException::new);
        if (!ator.id().equals(id) && !PerfilPermissoes.podeVer(ator.perfil(), usuario.getPerfil())) {
            // MASTER não pode nem saber que um DEV existe: responde como inexistente.
            throw new UsuarioNaoEncontradoException();
        }
        return usuario;
    }

    @Transactional
    public Usuario alterarStatus(UUID id, StatusUsuario novoStatus, UsuarioAutenticado ator) {
        if (!PerfilPermissoes.gerenciaUsuarios(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para alterar usuários");
        }
        if (ator.id().equals(id)) {
            throw new RegraNegocioException("Não é possível alterar o próprio status.");
        }
        Usuario usuario = repositorio.findById(id).orElseThrow(UsuarioNaoEncontradoException::new);
        if (!PerfilPermissoes.podeVer(ator.perfil(), usuario.getPerfil())) {
            throw new UsuarioNaoEncontradoException();
        }
        if (!PerfilPermissoes.podeAlterarStatus(ator.perfil(), usuario.getPerfil())) {
            throw new AccessDeniedException("Sem permissão para alterar o status deste usuário");
        }
        if (usuario.getStatus() == novoStatus) {
            return usuario;
        }
        usuario.alterarStatus(novoStatus, agora());
        if (novoStatus == StatusUsuario.INATIVO) {
            // D-068: inativo perde o acesso na hora (o UsuarioJwtConverter já barra o JWT; aqui caem as renovações).
            sessoes.revogarTodas(usuario.getId());
        }
        AcaoAuditoria acao =
                novoStatus == StatusUsuario.ATIVO ? AcaoAuditoria.USUARIO_ATIVADO : AcaoAuditoria.USUARIO_DESATIVADO;
        auditoria.registrar(RegistroAuditoria.sobre(ator.id(), acao, ENTIDADE, usuario.getId()));
        return usuario;
    }

    @Transactional
    public void trocarPropriaSenha(UsuarioAutenticado ator, String senhaAtual, String novaSenha) {
        Usuario usuario = repositorio.findById(ator.id()).orElseThrow(UsuarioNaoEncontradoException::new);
        if (senhaAtual == null || !codificador.matches(senhaAtual, usuario.senhaHash())) {
            throw new RegraNegocioException("Senha atual incorreta.");
        }
        if (senhaAtual.equals(novaSenha)) {
            throw new RegraNegocioException("A nova senha precisa ser diferente da atual.");
        }
        exigirSenhaValida(novaSenha);
        usuario.definirSenha(codificador.encode(novaSenha), false, agora());
        auditoria.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.SENHA_ALTERADA, ENTIDADE, usuario.getId()));
    }

    private Usuario salvarNovo(NovoUsuarioDto dados, UUID criadoPor) {
        validar(dados);
        String hash = hashCpf.calcular(dados.cpf());
        if (repositorio.existsByCpfHash(hash)) {
            throw new RegraNegocioException("Já existe um usuário com este CPF.");
        }
        if (repositorio.existsByEmailIgnoreCase(dados.email())) {
            throw new RegraNegocioException("Já existe um usuário com este e-mail.");
        }
        if (dados.perfil() == Perfil.PROFISSIONAL
                && repositorio.existsByConselhoAndNumeroRegistroAndUfRegistro(
                        dados.conselho(), dados.numeroRegistro(), dados.ufRegistro())) {
            throw new RegraNegocioException("Já existe um usuário com este registro profissional.");
        }
        Usuario usuario =
                new Usuario(dados, hash, codificador.encode(dados.senhaProvisoria()), criadoPor, agora());
        return repositorio.save(usuario);
    }

    private static void validar(NovoUsuarioDto dados) {
        if (dados.nomeCompleto() == null || dados.nomeCompleto().isBlank()) {
            throw new RegraNegocioException("Informe o nome completo.");
        }
        if (!CpfUtils.valido(dados.cpf())) {
            throw new RegraNegocioException("CPF inválido.");
        }
        if (dados.email() == null || !dados.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RegraNegocioException("E-mail inválido.");
        }
        if (dados.perfil() == null) {
            throw new RegraNegocioException("Informe o perfil.");
        }
        boolean temRegistro = dados.areaAtuacao() != null
                || dados.conselho() != null
                || dados.numeroRegistro() != null
                || dados.ufRegistro() != null;
        if (dados.perfil() == Perfil.PROFISSIONAL) {
            if (dados.areaAtuacao() == null
                    || dados.conselho() == null
                    || dados.numeroRegistro() == null
                    || dados.ufRegistro() == null) {
                throw new RegraNegocioException(
                        "Profissional precisa de área de atuação e registro profissional (conselho, número e UF).");
            }
            if (!UfValidator.valida(dados.ufRegistro())) {
                throw new RegraNegocioException("UF do registro profissional inválida.");
            }
        } else if (temRegistro) {
            throw new RegraNegocioException("Área de atuação e registro profissional são só para o perfil PROFISSIONAL.");
        }
        exigirSenhaValida(dados.senhaProvisoria());
    }

    private static void exigirSenhaValida(String senha) {
        List<String> violacoes = PoliticaSenhaValidator.violacoes(senha);
        if (!violacoes.isEmpty()) {
            throw new RegraNegocioException("A senha precisa " + String.join(", ", violacoes) + ".");
        }
    }

    private static NovoUsuarioDto normalizar(NovoUsuarioDto d) {
        return new NovoUsuarioDto(
                semEspacos(d.nomeCompleto()),
                CpfUtils.normalizar(d.cpf()),
                d.email() == null ? null : d.email().trim().toLowerCase(Locale.ROOT),
                vazioComoNulo(d.telefone()),
                d.perfil(),
                d.areaAtuacao(),
                d.conselho(),
                vazioComoNulo(d.numeroRegistro()),
                d.ufRegistro() == null ? null : vazioComoNulo(d.ufRegistro().toUpperCase(Locale.ROOT)),
                d.senhaProvisoria());
    }

    private static String semEspacos(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Instant agora() {
        return Instant.now(relogio);
    }
}
