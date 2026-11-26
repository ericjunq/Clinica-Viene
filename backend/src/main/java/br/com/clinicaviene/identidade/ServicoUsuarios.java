package br.com.clinicaviene.identidade;

import br.com.clinicaviene.auditoria.AcaoAuditoria;
import br.com.clinicaviene.auditoria.RegistroAuditoria;
import br.com.clinicaviene.auditoria.ServicoAuditoria;
import br.com.clinicaviene.compartilhado.cripto.Cpf;
import br.com.clinicaviene.compartilhado.cripto.HashCpf;
import br.com.clinicaviene.compartilhado.web.RecursoNaoEncontradoException;
import br.com.clinicaviene.compartilhado.web.RegraDeNegocioException;
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
 * método recebe quem está agindo ({@link UsuarioAutenticado}) e aplica {@link RegrasDePerfil}.
 */
@Service
public class ServicoUsuarios {

    private static final String ENTIDADE = "Usuario";

    private final RepositorioUsuarios repositorio;
    private final HashCpf hashCpf;
    private final PasswordEncoder codificador;
    private final ServicoAuditoria auditoria;
    private final Clock relogio;

    ServicoUsuarios(
            RepositorioUsuarios repositorio,
            HashCpf hashCpf,
            PasswordEncoder codificador,
            ServicoAuditoria auditoria,
            Clock relogio) {
        this.repositorio = repositorio;
        this.hashCpf = hashCpf;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public Usuario criar(DadosNovoUsuario dados, UsuarioAutenticado ator) {
        if (!RegrasDePerfil.gerenciaUsuarios(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para criar usuários");
        }
        if (dados.perfil() == Perfil.DEV) {
            throw new RegraDeNegocioException("Usuários DEV não podem ser criados pelo sistema.");
        }
        if (!RegrasDePerfil.podeCriar(ator.perfil(), dados.perfil())) {
            throw new AccessDeniedException("Sem permissão para criar este perfil");
        }
        Usuario usuario = salvarNovo(normalizar(dados), ator.id());
        auditoria.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.USUARIO_CRIADO, ENTIDADE, usuario.getId()));
        return usuario;
    }

    /** Usado só pelo inicializador, com o banco sem usuários (D-048). */
    @Transactional
    Usuario criarDevInicial(DadosNovoUsuario dados) {
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
        if (!RegrasDePerfil.gerenciaUsuarios(ator.perfil()) && !ator.id().equals(id)) {
            throw new AccessDeniedException("Sem permissão para ver usuários");
        }
        Usuario usuario = repositorio.findById(id).orElseThrow(UsuarioNaoEncontrado::new);
        if (!ator.id().equals(id) && !RegrasDePerfil.podeVer(ator.perfil(), usuario.getPerfil())) {
            // MASTER não pode nem saber que um DEV existe: responde como inexistente.
            throw new UsuarioNaoEncontrado();
        }
        return usuario;
    }

    @Transactional
    public Usuario alterarStatus(UUID id, StatusUsuario novoStatus, UsuarioAutenticado ator) {
        if (!RegrasDePerfil.gerenciaUsuarios(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para alterar usuários");
        }
        if (ator.id().equals(id)) {
            throw new RegraDeNegocioException("Não é possível alterar o próprio status.");
        }
        Usuario usuario = repositorio.findById(id).orElseThrow(UsuarioNaoEncontrado::new);
        if (!RegrasDePerfil.podeVer(ator.perfil(), usuario.getPerfil())) {
            throw new UsuarioNaoEncontrado();
        }
        if (!RegrasDePerfil.podeAlterarStatus(ator.perfil(), usuario.getPerfil())) {
            throw new AccessDeniedException("Sem permissão para alterar o status deste usuário");
        }
        if (usuario.getStatus() == novoStatus) {
            return usuario;
        }
        usuario.alterarStatus(novoStatus, agora());
        AcaoAuditoria acao =
                novoStatus == StatusUsuario.ATIVO ? AcaoAuditoria.USUARIO_ATIVADO : AcaoAuditoria.USUARIO_DESATIVADO;
        auditoria.registrar(RegistroAuditoria.sobre(ator.id(), acao, ENTIDADE, usuario.getId()));
        return usuario;
    }

    @Transactional
    public void trocarPropriaSenha(UsuarioAutenticado ator, String senhaAtual, String novaSenha) {
        Usuario usuario = repositorio.findById(ator.id()).orElseThrow(UsuarioNaoEncontrado::new);
        if (senhaAtual == null || !codificador.matches(senhaAtual, usuario.senhaHash())) {
            throw new RegraDeNegocioException("Senha atual incorreta.");
        }
        if (senhaAtual.equals(novaSenha)) {
            throw new RegraDeNegocioException("A nova senha precisa ser diferente da atual.");
        }
        exigirSenhaValida(novaSenha);
        usuario.definirSenha(codificador.encode(novaSenha), false, agora());
        auditoria.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.SENHA_ALTERADA, ENTIDADE, usuario.getId()));
    }

    private Usuario salvarNovo(DadosNovoUsuario dados, UUID criadoPor) {
        validar(dados);
        String hash = hashCpf.calcular(dados.cpf());
        if (repositorio.existsByCpfHash(hash)) {
            throw new RegraDeNegocioException("Já existe um usuário com este CPF.");
        }
        if (repositorio.existsByEmailIgnoreCase(dados.email())) {
            throw new RegraDeNegocioException("Já existe um usuário com este e-mail.");
        }
        if (dados.perfil() == Perfil.PROFISSIONAL
                && repositorio.existsByConselhoAndNumeroRegistroAndUfRegistro(
                        dados.conselho(), dados.numeroRegistro(), dados.ufRegistro())) {
            throw new RegraDeNegocioException("Já existe um usuário com este registro profissional.");
        }
        Usuario usuario =
                new Usuario(dados, hash, codificador.encode(dados.senhaProvisoria()), criadoPor, agora());
        return repositorio.save(usuario);
    }

    private static void validar(DadosNovoUsuario dados) {
        if (dados.nomeCompleto() == null || dados.nomeCompleto().isBlank()) {
            throw new RegraDeNegocioException("Informe o nome completo.");
        }
        if (!Cpf.valido(dados.cpf())) {
            throw new RegraDeNegocioException("CPF inválido.");
        }
        if (dados.email() == null || !dados.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RegraDeNegocioException("E-mail inválido.");
        }
        if (dados.perfil() == null) {
            throw new RegraDeNegocioException("Informe o perfil.");
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
                throw new RegraDeNegocioException(
                        "Profissional precisa de área de atuação e registro profissional (conselho, número e UF).");
            }
            if (!Uf.valida(dados.ufRegistro())) {
                throw new RegraDeNegocioException("UF do registro profissional inválida.");
            }
        } else if (temRegistro) {
            throw new RegraDeNegocioException("Área de atuação e registro profissional são só para o perfil PROFISSIONAL.");
        }
        exigirSenhaValida(dados.senhaProvisoria());
    }

    private static void exigirSenhaValida(String senha) {
        List<String> violacoes = PoliticaSenha.violacoes(senha);
        if (!violacoes.isEmpty()) {
            throw new RegraDeNegocioException("A senha precisa " + String.join(", ", violacoes) + ".");
        }
    }

    private static DadosNovoUsuario normalizar(DadosNovoUsuario d) {
        return new DadosNovoUsuario(
                semEspacos(d.nomeCompleto()),
                Cpf.normalizar(d.cpf()),
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

    static final class UsuarioNaoEncontrado extends RecursoNaoEncontradoException {
        UsuarioNaoEncontrado() {
            super("Usuário não encontrado.");
        }
    }
}
