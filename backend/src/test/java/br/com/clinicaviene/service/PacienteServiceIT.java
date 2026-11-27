package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.ResponsavelLegalDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.Parentesco;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.exceptions.PossivelDuplicidadeException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

/** Cadastro de pacientes contra o PostgreSQL real (CONTEXTO §4, D-053, D-091, D-092). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class PacienteServiceIT {

    @Autowired
    private PacienteService servico;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void preCadastraAdultoComCpfCifradoVersaoEAuditoria() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        String cpf = CpfsFicticios.gerar();
        String cpfFormatado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);

        Paciente paciente = servico.preCadastrar(
                adulto(cpfFormatado, "  Maria   da Silva ", " Maria@Exemplo.com "), recepcao);

        var linha = jdbc.queryForMap("SELECT * FROM paciente WHERE id = ?", paciente.getId());
        assertThat((String) linha.get("cpf")).startsWith("v2:").doesNotContain(cpf);
        assertThat(linha.get("cpf_hash")).isEqualTo(cpfHasher.calcular(cpf));
        assertThat(linha.get("nome_completo")).isEqualTo("Maria da Silva");
        assertThat(linha.get("email")).isEqualTo("maria@exemplo.com");
        assertThat(linha.get("cep")).isEqualTo("45000000");

        var versao = jdbc.queryForMap("SELECT * FROM paciente_versao WHERE paciente_id = ?", paciente.getId());
        assertThat((String) versao.get("dados")).startsWith("v2:").doesNotContain("Maria");
        assertThat(versao.get("origem")).isEqualTo(OrigemAlteracao.RECEPCAO.name());
        assertThat(contarAuditoria("PACIENTE_CRIADO", paciente.getId())).isEqualTo(1);
    }

    @Test
    void cpfDuplicadoOuInvalidoERecusado() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        String cpf = CpfsFicticios.gerar();
        servico.preCadastrar(adulto(cpf, "Primeiro", null), recepcao);

        assertThatThrownBy(() -> servico.preCadastrar(adulto(cpf, "Segundo", null), recepcao))
                .hasMessage("Já existe um paciente com este CPF.");
        assertThatThrownBy(() -> servico.preCadastrar(adulto("111.111.111-11", "Terceiro", null), recepcao))
                .hasMessage("CPF inválido.");
    }

    @Test
    void menorPrecisaDeResponsavelEPodeNaoTerCpf() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        LocalDate nascimento = servico.hoje().minusYears(8);

        assertThatThrownBy(() -> servico.preCadastrar(menor(null, nascimento, null, "Criança Sem Responsável"), recepcao))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("responsável legal");

        Paciente crianca = servico.preCadastrar(
                menor(null, nascimento, responsavel(CpfsFicticios.gerar()), "Criança Teste " + UUID.randomUUID()),
                recepcao);
        assertThat(crianca.getCpf()).isNull();
        assertThat(servico.responsavelDe(crianca.getId())).isPresent();
        String cpfResponsavel = jdbc.queryForObject(
                "SELECT cpf FROM responsavel_legal WHERE paciente_id = ?", String.class, crianca.getId());
        assertThat(cpfResponsavel).startsWith("v2:");
    }

    @Test
    void semCpfAlertaPossivelDuplicidadePorNomeNascimentoEResponsavel() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        LocalDate nascimento = servico.hoje().minusYears(5);
        String nome = "Gêmeo Teste " + UUID.randomUUID();
        String cpfResponsavel = CpfsFicticios.gerar();
        Paciente primeiro = servico.preCadastrar(menor(null, nascimento, responsavel(cpfResponsavel), nome), recepcao);

        assertThatThrownBy(() -> servico.preCadastrar(
                        menor(null, nascimento, responsavel(cpfResponsavel), nome.toUpperCase()), recepcao))
                .isInstanceOf(PossivelDuplicidadeException.class)
                .satisfies(e -> assertThat(((PossivelDuplicidadeException) e).getPacientesSemelhantes())
                        .containsExactly(primeiro.getId()));

        // a recepção conferiu que é outra pessoa: confirma e cria
        NovoPacienteRequest confirmado = comConfirmacao(menor(null, nascimento, responsavel(cpfResponsavel), nome));
        assertThat(servico.preCadastrar(confirmado, recepcao).getId()).isNotEqualTo(primeiro.getId());
    }

    @Test
    void adultoSemCpfSoComAprovacaoDoMasterEJustificativa() {
        NovoPacienteRequest semCpf = new NovoPacienteRequest(
                "Adulto Sem Cpf " + UUID.randomUUID(), null, servico.hoje().minusYears(40), "a@exemplo.com", null, null,
                null, "Estrangeiro sem CPF; apresentou passaporte.", false);

        assertThatThrownBy(() -> servico.preCadastrar(semCpf, usuario(Perfil.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);

        UsuarioAutenticado master = usuario(Perfil.MASTER);
        Paciente paciente = servico.preCadastrar(semCpf, master);
        assertThat(paciente.getSemCpfAprovadoPor()).isEqualTo(master.id());
        assertThat(jdbc.queryForObject(
                        "SELECT justificativa FROM audit_log WHERE acao = 'PACIENTE_SEM_CPF_APROVADO' AND entidade_id = ?",
                        String.class,
                        paciente.getId().toString()))
                .contains("passaporte");

        NovoPacienteRequest semJustificativa = new NovoPacienteRequest(
                "Outro Adulto " + UUID.randomUUID(), null, servico.hoje().minusYears(40), "b@exemplo.com", null, null,
                null, " ", false);
        assertThatThrownBy(() -> servico.preCadastrar(semJustificativa, master)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void validaNascimentoEContato() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        NovoPacienteRequest futuro = new NovoPacienteRequest(
                "Futuro", CpfsFicticios.gerar(), servico.hoje().plusDays(1), "f@exemplo.com", null, null, null, null, false);
        NovoPacienteRequest semContato = new NovoPacienteRequest(
                "Sem Contato", CpfsFicticios.gerar(), servico.hoje().minusYears(30), null, " ", null, null, null, false);

        assertThatThrownBy(() -> servico.preCadastrar(futuro, recepcao)).hasMessageContaining("futuro");
        assertThatThrownBy(() -> servico.preCadastrar(semContato, recepcao)).hasMessageContaining("contato");
    }

    @Test
    void atualizaContatoComVersaoAuditadaENaoMexeEmNomeCpfENascimento() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        Paciente paciente = servico.preCadastrar(adulto(CpfsFicticios.gerar(), "Contato Teste", "c@exemplo.com"), recepcao);

        Paciente alterado = servico.atualizarContato(
                paciente.getId(), "novo@exemplo.com", "71988887777",
                new EnderecoDto("40000-000", "Rua Nova", "10", null, "Centro", "Salvador", "ba"), recepcao);

        assertThat(alterado.getEmail()).isEqualTo("novo@exemplo.com");
        assertThat(alterado.getUf()).isEqualTo("BA");
        assertThat(alterado.getNomeCompleto()).isEqualTo("Contato Teste");
        var historico = servico.historico(paciente.getId(), recepcao);
        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).getCamposAlterados()).isEqualTo("e-mail, telefone, endereço");
        assertThat(contarAuditoria("PACIENTE_ALTERADO", paciente.getId())).isEqualTo(1);

        // sem mudança real: nenhuma versão nova
        servico.atualizarContato(paciente.getId(), "novo@exemplo.com", "71988887777", alterado.endereco(), recepcao);
        assertThat(servico.historico(paciente.getId(), recepcao)).hasSize(2);
    }

    @Test
    void historicoESomenteInclusaoNoBanco() {
        Paciente paciente = servico.preCadastrar(adulto(CpfsFicticios.gerar(), "Historico", null), usuario(Perfil.ADMIN));

        assertThatThrownBy(() -> jdbc.update("UPDATE paciente_versao SET campos_alterados = 'x' WHERE paciente_id = ?", paciente.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inclusão");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM paciente_versao WHERE paciente_id = ?", paciente.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inclusão");
    }

    @Test
    void pesquisaPorCpfNomeOuNascimentoEAuditaSemOTermo() {
        UsuarioAutenticado recepcao = usuario(Perfil.MASTER);
        String cpf = CpfsFicticios.gerar();
        String nome = "Busca " + UUID.randomUUID().toString().substring(0, 8);
        LocalDate nascimento = LocalDate.of(1971, 3, 9);
        Paciente paciente = servico.preCadastrar(
                new NovoPacienteRequest(nome, cpf, nascimento, "b@exemplo.com", null, null, null, null, false), recepcao);

        assertThat(servico.pesquisar(cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9), recepcao))
                .extracting(Paciente::getId)
                .containsExactly(paciente.getId());
        assertThat(servico.pesquisar(nome.toLowerCase(), recepcao)).extracting(Paciente::getId).contains(paciente.getId());
        assertThat(servico.pesquisar("09/03/1971", recepcao)).extracting(Paciente::getId).contains(paciente.getId());
        assertThat(servico.pesquisar("x", recepcao)).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit_log WHERE acao = 'PACIENTE_PESQUISADO' AND usuario_id = ? AND justificativa IS NULL",
                        Integer.class,
                        recepcao.id()))
                .isEqualTo(4);
    }

    @Test
    void soRecepcaoEMasterAcessamOCadastroELeituraEAuditada() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        Paciente paciente = servico.preCadastrar(adulto(CpfsFicticios.gerar(), "Acesso", null), recepcao);

        assertThatThrownBy(() -> servico.buscar(paciente.getId(), usuario(Perfil.DEV)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servico.buscar(paciente.getId(), usuario(Perfil.PROFISSIONAL)))
                .isInstanceOf(AccessDeniedException.class);

        servico.buscar(paciente.getId(), recepcao);
        assertThat(contarAuditoria("PACIENTE_LIDO", paciente.getId())).isEqualTo(1);
    }

    // --- apoio ---

    private NovoPacienteRequest adulto(String cpf, String nome, String email) {
        return new NovoPacienteRequest(
                nome,
                cpf,
                servico.hoje().minusYears(35),
                email == null ? "p." + UUID.randomUUID() + "@exemplo.com" : email,
                null,
                new EnderecoDto("45000-000", "Rua A", "1", null, "Centro", "Vitória da Conquista", "BA"),
                null,
                null,
                false);
    }

    private static NovoPacienteRequest menor(String cpf, LocalDate nascimento, ResponsavelLegalDto responsavel, String nome) {
        return new NovoPacienteRequest(nome, cpf, nascimento, null, "77999990000", null, responsavel, null, false);
    }

    private static NovoPacienteRequest comConfirmacao(NovoPacienteRequest r) {
        return new NovoPacienteRequest(
                r.nomeCompleto(), r.cpf(), r.dataNascimento(), r.email(), r.telefone(), r.endereco(), r.responsavel(),
                r.semCpfJustificativa(), true);
    }

    private static ResponsavelLegalDto responsavel(String cpf) {
        return new ResponsavelLegalDto("Mãe Teste", cpf, Parentesco.MAE, "77999991111", null);
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, Instant.now()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }

    private int contarAuditoria(String acao, UUID entidadeId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE acao = ? AND entidade_id = ?", Integer.class, acao, entidadeId.toString());
        return total == null ? 0 : total;
    }
}
