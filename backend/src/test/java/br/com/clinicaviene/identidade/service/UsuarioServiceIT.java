package br.com.clinicaviene.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.compartilhado.cripto.CpfHasher;
import br.com.clinicaviene.compartilhado.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.compartilhado.exceptions.RegraNegocioException;
import br.com.clinicaviene.identidade.dto.NovoUsuarioDto;
import br.com.clinicaviene.identidade.dto.UsuarioAutenticado;
import br.com.clinicaviene.identidade.dto.UsuarioResponse;
import br.com.clinicaviene.identidade.entity.Usuario;
import br.com.clinicaviene.identidade.enums.AreaAtuacao;
import br.com.clinicaviene.identidade.enums.ConselhoProfissional;
import br.com.clinicaviene.identidade.enums.Perfil;
import br.com.clinicaviene.identidade.enums.StatusUsuario;
import br.com.clinicaviene.identidade.repository.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Regras de usuários contra o PostgreSQL real (CONTEXTO §2, D-061, D-066 a D-069). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
public class UsuarioServiceIT {

    private static final String SENHA = "Senha#Provisoria1";

    @Autowired
    private UsuarioService servico;

    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private CpfHasher hashCpf;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void criaProfissionalComCpfCifradoHashESenhaArgon2() {
        UsuarioAutenticado master = existente(Perfil.MASTER);
        String cpf = CpfsFicticios.gerar();
        String cpfFormatado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-"
                + cpf.substring(9);

        String sufixo = UUID.randomUUID().toString();
        Usuario criado = servico.criar(
                new NovoUsuarioDto(
                        "  Ana   Psicóloga ", cpfFormatado, " Ana." + sufixo + "@Exemplo.com ", null,
                        Perfil.PROFISSIONAL, AreaAtuacao.PSICOLOGIA, ConselhoProfissional.CRP, numeroUnico(), "ba",
                        SENHA),
                master);

        var linha = jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", criado.getId());
        assertThat((String) linha.get("cpf")).startsWith("v2:").doesNotContain(cpf);
        assertThat(linha.get("cpf_hash")).isEqualTo(hashCpf.calcular(cpf));
        assertThat((String) linha.get("senha_hash")).startsWith("$argon2id$");
        assertThat(linha.get("nome_completo")).isEqualTo("Ana Psicóloga");
        assertThat(linha.get("email")).isEqualTo("ana." + sufixo + "@exemplo.com");
        assertThat(linha.get("uf_registro")).isEqualTo("BA");
        assertThat(linha.get("status")).isEqualTo("ATIVO");
        assertThat(linha.get("trocar_senha")).isEqualTo(true);
        assertThat(linha.get("criado_por")).isEqualTo(master.id());
        assertThat(repositorio.findByCpfHash(hashCpf.calcular(cpf)).orElseThrow().getCpf()).isEqualTo(cpf);
        assertThat(contarAuditoria("USUARIO_CRIADO", criado.getId())).isEqualTo(1);
    }

    @Test
    void devCriaMasterEMasterCriaMaster() {
        UsuarioAutenticado dev = existente(Perfil.DEV);

        Usuario master = servico.criar(dados(Perfil.MASTER), dev);
        Usuario outroMaster = servico.criar(dados(Perfil.MASTER), autenticado(master));

        assertThat(outroMaster.getPerfil()).isEqualTo(Perfil.MASTER);
    }

    @Test
    void ninguemCriaDev() {
        assertThatThrownBy(() -> servico.criar(dados(Perfil.DEV), existente(Perfil.DEV)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.criar(dados(Perfil.DEV), existente(Perfil.MASTER)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void adminEProfissionalNaoCriamUsuarios() {
        assertThatThrownBy(() -> servico.criar(dados(Perfil.ADMIN), existente(Perfil.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servico.criar(dados(Perfil.ADMIN), existente(Perfil.PROFISSIONAL)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recusaCpfInvalidoDuplicadoEmailDuplicadoESenhaFraca() {
        UsuarioAutenticado master = existente(Perfil.MASTER);
        NovoUsuarioDto base = dados(Perfil.ADMIN);
        servico.criar(base, master);

        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "cpf", "111.111.111-11"), master))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "cpf", base.cpf()), master))
                .hasMessage("Já existe um usuário com este CPF.");
        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "email", base.email().toUpperCase()), master))
                .hasMessage("Já existe um usuário com este e-mail.");
        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "senha", "fraca"), master))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("12 caracteres");
    }

    @Test
    void registroProfissionalObrigatorioSoParaProfissional() {
        UsuarioAutenticado master = existente(Perfil.MASTER);
        NovoUsuarioDto profissionalSemRegistro = new NovoUsuarioDto(
                "Pro Sem Registro", CpfsFicticios.gerar(), emailUnico(), null, Perfil.PROFISSIONAL, null, null, null,
                null, SENHA);
        NovoUsuarioDto adminComRegistro = new NovoUsuarioDto(
                "Admin Com Registro", CpfsFicticios.gerar(), emailUnico(), null, Perfil.ADMIN,
                AreaAtuacao.ODONTOLOGIA, ConselhoProfissional.CRO, numeroUnico(), "BA", SENHA);
        NovoUsuarioDto ufInvalida = new NovoUsuarioDto(
                "Pro UF Errada", CpfsFicticios.gerar(), emailUnico(), null, Perfil.PROFISSIONAL,
                AreaAtuacao.ODONTOLOGIA, ConselhoProfissional.CRO, numeroUnico(), "XX", SENHA);

        assertThatThrownBy(() -> servico.criar(profissionalSemRegistro, master)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.criar(adminComRegistro, master)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.criar(ufInvalida, master)).hasMessage("UF do registro profissional inválida.");
    }

    @Test
    void registroProfissionalEhUnicoPorConselhoNumeroEUf() {
        UsuarioAutenticado master = existente(Perfil.MASTER);
        String numero = numeroUnico();
        servico.criar(profissional(numero), master);

        assertThatThrownBy(() -> servico.criar(profissional(numero), master))
                .hasMessage("Já existe um usuário com este registro profissional.");
    }

    @Test
    void masterNaoVeNemDesativaDev() {
        UsuarioAutenticado dev = existente(Perfil.DEV);
        UsuarioAutenticado master = existente(Perfil.MASTER);

        assertThat(servico.listar(master)).extracting(Usuario::getPerfil).doesNotContain(Perfil.DEV);
        assertThat(servico.listar(dev)).extracting(Usuario::getId).contains(dev.id(), master.id());
        assertThatThrownBy(() -> servico.buscar(dev.id(), master)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.alterarStatus(dev.id(), StatusUsuario.INATIVO, master))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void devNaoDesativaMasterMasDesativaAdminEProfissional() {
        UsuarioAutenticado dev = existente(Perfil.DEV);
        UsuarioAutenticado master = existente(Perfil.MASTER);
        UsuarioAutenticado admin = existente(Perfil.ADMIN);

        assertThatThrownBy(() -> servico.alterarStatus(master.id(), StatusUsuario.INATIVO, dev))
                .isInstanceOf(AccessDeniedException.class);

        Usuario desativado = servico.alterarStatus(admin.id(), StatusUsuario.INATIVO, dev);
        assertThat(desativado.getStatus()).isEqualTo(StatusUsuario.INATIVO);
        assertThat(contarAuditoria("USUARIO_DESATIVADO", admin.id())).isEqualTo(1);

        Usuario reativado = servico.alterarStatus(admin.id(), StatusUsuario.ATIVO, master);
        assertThat(reativado.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(contarAuditoria("USUARIO_ATIVADO", admin.id())).isEqualTo(1);
    }

    @Test
    void ninguemAlteraOProprioStatusEAdminNaoGerencia() {
        UsuarioAutenticado master = existente(Perfil.MASTER);
        UsuarioAutenticado admin = existente(Perfil.ADMIN);

        assertThatThrownBy(() -> servico.alterarStatus(master.id(), StatusUsuario.INATIVO, master))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.alterarStatus(master.id(), StatusUsuario.INATIVO, admin))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servico.listar(admin)).isInstanceOf(AccessDeniedException.class);
        assertThat(servico.buscar(admin.id(), admin).getId()).isEqualTo(admin.id());
        assertThatThrownBy(() -> servico.buscar(master.id(), admin)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void trocaAPropriaSenhaEDesligaATrocaObrigatoria() {
        UsuarioAutenticado admin = existente(Perfil.ADMIN);

        assertThatThrownBy(() -> servico.trocarPropriaSenha(admin, "Errada#Senha123", "Nova#Senha1234"))
                .hasMessage("Senha atual incorreta.");
        assertThatThrownBy(() -> servico.trocarPropriaSenha(admin, SENHA, SENHA))
                .hasMessage("A nova senha precisa ser diferente da atual.");
        assertThatThrownBy(() -> servico.trocarPropriaSenha(admin, SENHA, "fraca"))
                .hasMessageContaining("12 caracteres");

        servico.trocarPropriaSenha(admin, SENHA, "Nova#Senha1234");

        Usuario depois = repositorio.findById(admin.id()).orElseThrow();
        assertThat(depois.isTrocarSenha()).isFalse();
        assertThat(codificador.matches("Nova#Senha1234", depois.senhaHash())).isTrue();
        assertThat(contarAuditoria("SENHA_ALTERADA", admin.id())).isEqualTo(1);
    }

    @Test
    void mascaraOCpfNaResposta() {
        assertThat(UsuarioResponse.mascararCpf("52601815906")).isEqualTo("***.018.159-**");
        assertThat(UsuarioResponse.mascararCpf(null)).isNull();
    }

    // --- apoio ---

    /** Grava direto no banco um usuário do perfil pedido (ator dos testes). */
    private UsuarioAutenticado existente(Perfil perfil) {
        NovoUsuarioDto dados = perfil == Perfil.PROFISSIONAL ? profissional(numeroUnico()) : dados(perfil);
        Usuario usuario = new Usuario(
                dados, hashCpf.calcular(dados.cpf()), codificador.encode(SENHA), null, Instant.now());
        repositorio.saveAndFlush(usuario);
        return autenticado(usuario);
    }

    private static UsuarioAutenticado autenticado(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getPerfil());
    }

    private static NovoUsuarioDto dados(Perfil perfil) {
        return new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), emailUnico(), null, perfil, null, null, null, null, SENHA);
    }

    private static NovoUsuarioDto profissional(String numero) {
        return new NovoUsuarioDto(
                "Profissional " + numero, CpfsFicticios.gerar(), emailUnico(), null, Perfil.PROFISSIONAL,
                AreaAtuacao.ODONTOLOGIA, ConselhoProfissional.CRO, numero, "BA", SENHA);
    }

    private static NovoUsuarioDto com(NovoUsuarioDto d, String campo, String valor) {
        return new NovoUsuarioDto(
                d.nomeCompleto(),
                campo.equals("cpf") ? valor : d.cpf(),
                campo.equals("email") ? valor : d.email(),
                d.telefone(),
                d.perfil(),
                d.areaAtuacao(),
                d.conselho(),
                d.numeroRegistro(),
                d.ufRegistro(),
                campo.equals("senha") ? valor : d.senhaProvisoria());
    }

    private static String emailUnico() {
        return "teste." + UUID.randomUUID() + "@exemplo.com";
    }

    private static String numeroUnico() {
        return String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L));
    }

    private int contarAuditoria(String acao, UUID entidadeId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE acao = ? AND entidade_id = ?",
                Integer.class,
                acao,
                entidadeId.toString());
        return total == null ? 0 : total;
    }
}
