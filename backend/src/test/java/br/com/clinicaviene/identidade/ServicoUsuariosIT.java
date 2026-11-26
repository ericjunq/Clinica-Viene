package br.com.clinicaviene.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.compartilhado.cripto.HashCpf;
import br.com.clinicaviene.compartilhado.web.RecursoNaoEncontradoException;
import br.com.clinicaviene.compartilhado.web.RegraDeNegocioException;
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
class ServicoUsuariosIT {

    private static final String SENHA = "Senha#Provisoria1";

    @Autowired
    private ServicoUsuarios servico;

    @Autowired
    private RepositorioUsuarios repositorio;

    @Autowired
    private HashCpf hashCpf;

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
                new DadosNovoUsuario(
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
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.criar(dados(Perfil.DEV), existente(Perfil.MASTER)))
                .isInstanceOf(RegraDeNegocioException.class);
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
        DadosNovoUsuario base = dados(Perfil.ADMIN);
        servico.criar(base, master);

        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "cpf", "111.111.111-11"), master))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "cpf", base.cpf()), master))
                .hasMessage("Já existe um usuário com este CPF.");
        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "email", base.email().toUpperCase()), master))
                .hasMessage("Já existe um usuário com este e-mail.");
        assertThatThrownBy(() -> servico.criar(com(dados(Perfil.ADMIN), "senha", "fraca"), master))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("12 caracteres");
    }

    @Test
    void registroProfissionalObrigatorioSoParaProfissional() {
        UsuarioAutenticado master = existente(Perfil.MASTER);
        DadosNovoUsuario profissionalSemRegistro = new DadosNovoUsuario(
                "Pro Sem Registro", CpfsFicticios.gerar(), emailUnico(), null, Perfil.PROFISSIONAL, null, null, null,
                null, SENHA);
        DadosNovoUsuario adminComRegistro = new DadosNovoUsuario(
                "Admin Com Registro", CpfsFicticios.gerar(), emailUnico(), null, Perfil.ADMIN,
                AreaAtuacao.ODONTOLOGIA, ConselhoProfissional.CRO, numeroUnico(), "BA", SENHA);
        DadosNovoUsuario ufInvalida = new DadosNovoUsuario(
                "Pro UF Errada", CpfsFicticios.gerar(), emailUnico(), null, Perfil.PROFISSIONAL,
                AreaAtuacao.ODONTOLOGIA, ConselhoProfissional.CRO, numeroUnico(), "XX", SENHA);

        assertThatThrownBy(() -> servico.criar(profissionalSemRegistro, master)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.criar(adminComRegistro, master)).isInstanceOf(RegraDeNegocioException.class);
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
                .isInstanceOf(RegraDeNegocioException.class);
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
        assertThat(ControladorUsuarios.UsuarioResposta.mascarar("52601815906")).isEqualTo("***.018.159-**");
        assertThat(ControladorUsuarios.UsuarioResposta.mascarar(null)).isNull();
    }

    // --- apoio ---

    /** Grava direto no banco um usuário do perfil pedido (ator dos testes). */
    private UsuarioAutenticado existente(Perfil perfil) {
        DadosNovoUsuario dados = perfil == Perfil.PROFISSIONAL ? profissional(numeroUnico()) : dados(perfil);
        Usuario usuario = new Usuario(
                dados, hashCpf.calcular(dados.cpf()), codificador.encode(SENHA), null, Instant.now());
        repositorio.saveAndFlush(usuario);
        return autenticado(usuario);
    }

    private static UsuarioAutenticado autenticado(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getPerfil());
    }

    private static DadosNovoUsuario dados(Perfil perfil) {
        return new DadosNovoUsuario(
                "Pessoa " + perfil, CpfsFicticios.gerar(), emailUnico(), null, perfil, null, null, null, null, SENHA);
    }

    private static DadosNovoUsuario profissional(String numero) {
        return new DadosNovoUsuario(
                "Profissional " + numero, CpfsFicticios.gerar(), emailUnico(), null, Perfil.PROFISSIONAL,
                AreaAtuacao.ODONTOLOGIA, ConselhoProfissional.CRO, numero, "BA", SENHA);
    }

    private static DadosNovoUsuario com(DadosNovoUsuario d, String campo, String valor) {
        return new DadosNovoUsuario(
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
