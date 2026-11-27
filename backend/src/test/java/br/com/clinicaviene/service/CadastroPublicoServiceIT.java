package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.CodigoCadastroEnviadoResponse;
import br.com.clinicaviene.dto.ConviteAbertoResponse;
import br.com.clinicaviene.dto.DadosCadastroDto;
import br.com.clinicaviene.dto.DecisaoTermoRequest;
import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.dto.FichaCadastroRequest;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.ResponsavelLegalDto;
import br.com.clinicaviene.dto.RespostaAnamneseDto;
import br.com.clinicaviene.dto.SessaoCadastroResponse;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.Parentesco;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.exceptions.CodigoInvalidoException;
import br.com.clinicaviene.exceptions.ConviteInvalidoException;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.exceptions.SessaoCadastroInvalidaException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Fluxo público do cadastro (D-091, D-100): identidade, sessão de preenchimento, formulário e revisão. */
@Import({TestcontainersConfiguration.class, RelogioAjustavel.Configuracao.class})
@SpringBootTest
@ActiveProfiles("test")
class CadastroPublicoServiceIT {

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final String IP = "203.0.113.10";
    private static final String APARELHO = "Navegador de teste";

    @Autowired
    private CadastroPublicoService cadastroPublicoService;

    @Autowired
    private ConviteService conviteService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private TermoService termoService;

    @Autowired
    private ModeloAnamneseService modeloAnamneseService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private JdbcTemplate jdbc;

    private UsuarioAutenticado recepcao;

    @BeforeEach
    void publicarTermosEModeloSeFaltarem() {
        recepcao = usuario(Perfil.ADMIN);
        UsuarioAutenticado master = usuario(Perfil.MASTER);
        if (termoService.vigente(TipoTermo.POLITICA_PRIVACIDADE).isEmpty()) {
            TermoVersao t = termoService.criarRascunho(TipoTermo.POLITICA_PRIVACIDADE, "Política", "Texto da política", master);
            termoService.publicar(t.getId(), master);
        }
        if (modeloAnamneseService.vigente(AreaAtuacao.ODONTOLOGIA).isEmpty()) {
            ModeloAnamnese m = modeloAnamneseService.criarRascunho(
                    AreaAtuacao.ODONTOLOGIA,
                    "Ficha de teste",
                    List.of(
                            new PerguntaAnamneseDto(
                                    "alergia", "Saúde", "Tem alergia?", TipoPergunta.SIM_NAO_DETALHE, true, null, true, true,
                                    null),
                            new PerguntaAnamneseDto(
                                    "pressao", "Saúde", "Sua pressão é", TipoPergunta.ESCOLHA_UNICA, true,
                                    List.of("Normal", "Alta", "Baixa"), true, null, null)),
                    master);
            modeloAnamneseService.publicar(m.getId(), master);
        }
    }

    @Test
    void linkConfirmaPorCodigoNoEmailECarregaOFormulario() {
        String email = "ana." + UUID.randomUUID() + "@exemplo.com";
        UUID paciente = adulto(email);
        String convite = convite(paciente, ModoConvite.LINK);

        ConviteAbertoResponse aberto = cadastroPublicoService.abrir(convite, IP);
        assertThat(aberto.precisaCodigo()).isTrue();
        assertThat(aberto.primeiroNome()).isEqualTo("Ana");
        assertThat(aberto.emailMascarado()).isEqualTo("a***@exemplo.com");

        CodigoCadastroEnviadoResponse enviado = cadastroPublicoService.enviarCodigo(convite, IP, APARELHO);
        assertThat(Duration.between(relogio.instant(), enviado.podeReenviarEm())).isEqualTo(Duration.ofMinutes(1));
        String codigo = ultimoCodigo(email);

        assertThatThrownBy(() -> cadastroPublicoService.confirmarCodigo(convite, outroCodigo(codigo), IP, APARELHO))
                .isInstanceOf(CodigoInvalidoException.class);
        SessaoCadastroResponse sessao = cadastroPublicoService.confirmarCodigo(convite, codigo, IP, APARELHO);
        assertThat(Duration.between(relogio.instant(), sessao.expiraEm())).isEqualTo(Duration.ofHours(2));
        // o código vale uma vez
        assertThatThrownBy(() -> cadastroPublicoService.confirmarCodigo(convite, codigo, IP, APARELHO))
                .isInstanceOf(CodigoInvalidoException.class);

        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao.token());
        assertThat(formulario.modo()).isEqualTo(ModoConvite.LINK);
        assertThat(formulario.menorDeIdade()).isFalse();
        assertThat(formulario.dados().email()).isEqualTo(email);
        assertThat(formulario.anamnese().perguntas()).isNotEmpty();
        assertThat(formulario.termos()).anyMatch(t -> t.tipo() == TipoTermo.POLITICA_PRIVACIDADE && t.obrigatorio());

        assertThat(cadastroPublicoService.revisar(convite, sessao.token(), fichaCompleta(formulario)).prontoParaAssinar())
                .isTrue();

        assertThat(contarAuditoria("CADASTRO_CODIGO_INCORRETO", paciente)).isEqualTo(1);
        assertThat(contarAuditoria("CADASTRO_IDENTIDADE_CONFIRMADA", paciente)).isEqualTo(1);
        assertThat(contarAuditoria("CADASTRO_FORMULARIO_LIDO", paciente)).isEqualTo(1);
        assertThat(contarAuditoria("CADASTRO_REVISADO", paciente)).isEqualTo(1);
        Integer comCodigoPuro = jdbc.queryForObject(
                "SELECT count(*) FROM sessao_cadastro WHERE codigo_hash = ? OR token_hash = ?", Integer.class, codigo,
                sessao.token());
        assertThat(comCodigoPuro).isZero();
    }

    @Test
    void codigoErradoCincoVezesInvalidaEReenvioRespeitaAEspera() {
        String email = "bia." + UUID.randomUUID() + "@exemplo.com";
        String convite = convite(adulto(email), ModoConvite.LINK);

        cadastroPublicoService.enviarCodigo(convite, IP, APARELHO);
        String codigo = ultimoCodigo(email);
        assertThatThrownBy(() -> cadastroPublicoService.enviarCodigo(convite, IP, APARELHO))
                .isInstanceOf(MuitasTentativasException.class);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> cadastroPublicoService.confirmarCodigo(convite, outroCodigo(codigo), IP, APARELHO))
                    .isInstanceOf(CodigoInvalidoException.class);
        }
        // esgotou: nem o certo vale mais
        assertThatThrownBy(() -> cadastroPublicoService.confirmarCodigo(convite, codigo, IP, APARELHO))
                .isInstanceOf(CodigoInvalidoException.class);

        relogio.avancar(Duration.ofMinutes(1));
        cadastroPublicoService.enviarCodigo(convite, IP, APARELHO);
        assertThatCode(() -> cadastroPublicoService.confirmarCodigo(convite, ultimoCodigo(email), IP, APARELHO))
                .doesNotThrowAnyException();
    }

    @Test
    void codigoExpiraEmDezMinutos() {
        String email = "caio." + UUID.randomUUID() + "@exemplo.com";
        String convite = convite(adulto(email), ModoConvite.LINK);
        cadastroPublicoService.enviarCodigo(convite, IP, APARELHO);
        relogio.avancar(Duration.ofMinutes(10));
        assertThatThrownBy(() -> cadastroPublicoService.confirmarCodigo(convite, ultimoCodigo(email), IP, APARELHO))
                .isInstanceOf(CodigoInvalidoException.class);
    }

    @Test
    void quiosqueAbreSemCodigoELinkNaoPulaOCodigo() {
        UUID paciente = adulto("davi." + UUID.randomUUID() + "@exemplo.com");
        String link = convite(paciente, ModoConvite.LINK);
        assertThatThrownBy(() -> cadastroPublicoService.iniciarQuiosque(link, IP, APARELHO))
                .isInstanceOf(RegraNegocioException.class);

        String quiosque = convite(paciente, ModoConvite.QUIOSQUE);
        ConviteAbertoResponse aberto = cadastroPublicoService.abrir(quiosque, IP);
        assertThat(aberto.precisaCodigo()).isFalse();
        assertThat(aberto.emailMascarado()).isNull();
        assertThatThrownBy(() -> cadastroPublicoService.enviarCodigo(quiosque, IP, APARELHO))
                .isInstanceOf(ConviteInvalidoException.class);

        SessaoCadastroResponse sessao = cadastroPublicoService.iniciarQuiosque(quiosque, IP, APARELHO);
        assertThat(cadastroPublicoService.formulario(quiosque, sessao.token()).modo()).isEqualTo(ModoConvite.QUIOSQUE);
        assertThat(contarAuditoria("CADASTRO_QUIOSQUE_INICIADO", paciente)).isEqualTo(1);
    }

    @Test
    void sessaoSoValeParaOProprioConviteDentroDoPrazoEEnquantoEleNaoForCancelado() {
        UUID paciente = adulto("eva." + UUID.randomUUID() + "@exemplo.com");
        String convite = convite(paciente, ModoConvite.QUIOSQUE);
        String sessao = cadastroPublicoService.iniciarQuiosque(convite, IP, APARELHO).token();

        assertThatThrownBy(() -> cadastroPublicoService.formulario(convite, "sessao-falsa"))
                .isInstanceOf(SessaoCadastroInvalidaException.class);
        assertThatThrownBy(() -> cadastroPublicoService.formulario(convite, null))
                .isInstanceOf(SessaoCadastroInvalidaException.class);
        String outroConvite = convite(adulto("fabio." + UUID.randomUUID() + "@exemplo.com"), ModoConvite.QUIOSQUE);
        assertThatThrownBy(() -> cadastroPublicoService.formulario(outroConvite, sessao))
                .isInstanceOf(SessaoCadastroInvalidaException.class);

        relogio.avancar(Duration.ofHours(2));
        assertThatThrownBy(() -> cadastroPublicoService.formulario(convite, sessao))
                .isInstanceOf(SessaoCadastroInvalidaException.class);

        String novo = convite(paciente, ModoConvite.QUIOSQUE); // cancela o anterior
        String novaSessao = cadastroPublicoService.iniciarQuiosque(novo, IP, APARELHO).token();
        assertThatCode(() -> cadastroPublicoService.formulario(novo, novaSessao)).doesNotThrowAnyException();
        convite(paciente, ModoConvite.QUIOSQUE);
        assertThatThrownBy(() -> cadastroPublicoService.formulario(novo, novaSessao))
                .isInstanceOf(SessaoCadastroInvalidaException.class);
    }

    @Test
    void revisaoRecusaFichaIncompletaOuDesatualizada() {
        UUID paciente = adulto("gil." + UUID.randomUUID() + "@exemplo.com");
        String convite = convite(paciente, ModoConvite.QUIOSQUE);
        String sessao = cadastroPublicoService.iniciarQuiosque(convite, IP, APARELHO).token();
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        FichaCadastroRequest completa = fichaCompleta(formulario);

        DadosCadastroDto d = completa.dados();
        DadosCadastroDto semEndereco = new DadosCadastroDto(
                d.nomeCompleto(), d.cpf(), d.dataNascimento(), d.email(), d.telefone(), null, null);
        assertRecusa(convite, sessao, comDados(completa, semEndereco), "endereço completo");

        String cpfDeOutro = CpfsFicticios.gerar();
        adulto("outro." + UUID.randomUUID() + "@exemplo.com", cpfDeOutro);
        DadosCadastroDto comCpfDeOutro = new DadosCadastroDto(
                d.nomeCompleto(), cpfDeOutro, d.dataNascimento(), d.email(), d.telefone(), d.endereco(), null);
        assertRecusa(convite, sessao, comDados(completa, comCpfDeOutro), "outro cadastro");

        DadosCadastroDto semCpf = new DadosCadastroDto(
                d.nomeCompleto(), null, d.dataNascimento(), d.email(), d.telefone(), d.endereco(), null);
        assertRecusa(convite, sessao, comDados(completa, semCpf), "Informe o CPF");

        List<DecisaoTermoRequest> semPolitica = completa.termos().stream()
                .map(t -> new DecisaoTermoRequest(t.termoVersaoId(), ehPolitica(formulario, t.termoVersaoId()) ? false : t.aceito()))
                .toList();
        assertRecusa(convite, sessao, comTermos(completa, semPolitica), "política de privacidade");
        assertRecusa(convite, sessao, comTermos(completa, completa.termos().subList(1, completa.termos().size())),
                "termos foram atualizados");

        FichaCadastroRequest modeloAntigo = new FichaCadastroRequest(
                completa.dados(), UUID.randomUUID(), completa.respostas(), completa.termos(), null);
        assertRecusa(convite, sessao, modeloAntigo, "ficha de saúde foi atualizada");

        FichaCadastroRequest semRespostas = new FichaCadastroRequest(
                completa.dados(), completa.modeloAnamneseId(), Map.of(), completa.termos(), null);
        assertThatThrownBy(() -> cadastroPublicoService.revisar(convite, sessao, semRespostas))
                .isInstanceOf(RegraNegocioException.class);

        assertThatCode(() -> cadastroPublicoService.revisar(convite, sessao, completa)).doesNotThrowAnyException();
    }

    @Test
    void menorPreenchidoPeloResponsavel() {
        String emailMae = "mae." + UUID.randomUUID() + "@exemplo.com";
        ResponsavelLegalDto mae =
                new ResponsavelLegalDto("Mãe Cadastro", CpfsFicticios.gerar(), Parentesco.MAE, "77999991111", emailMae);
        UUID crianca = pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Lia Cadastro " + UUID.randomUUID(), null, pacienteService.hoje().minusYears(9), null,
                                "77999990000", null, mae, null, true),
                        recepcao)
                .getId();
        String convite = convite(crianca, ModoConvite.LINK);
        assertThat(cadastroPublicoService.abrir(convite, IP).emailMascarado()).isEqualTo("m***@exemplo.com");
        cadastroPublicoService.enviarCodigo(convite, IP, APARELHO);
        String sessao = cadastroPublicoService.confirmarCodigo(convite, ultimoCodigo(emailMae), IP, APARELHO).token();

        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        assertThat(formulario.menorDeIdade()).isTrue();
        assertThat(formulario.dados().responsavel().nomeCompleto()).isEqualTo("Mãe Cadastro");

        FichaCadastroRequest ficha = fichaCompleta(formulario);
        assertThat(cadastroPublicoService.revisar(convite, sessao, ficha).peloResponsavel()).isTrue();

        DadosCadastroDto d = ficha.dados();
        DadosCadastroDto semResponsavel = new DadosCadastroDto(
                d.nomeCompleto(), null, d.dataNascimento(), d.email(), d.telefone(), d.endereco(), null);
        assertRecusa(convite, sessao, comDados(ficha, semResponsavel), "responsável legal");
    }

    @Test
    void conviteInvalidoTemSempreAMesmaResposta() {
        assertThatThrownBy(() -> cadastroPublicoService.abrir("nao-existe", IP)).isInstanceOf(ConviteInvalidoException.class);
        assertThatThrownBy(() -> cadastroPublicoService.abrir(null, IP)).isInstanceOf(ConviteInvalidoException.class);
        UUID paciente = adulto("hugo." + UUID.randomUUID() + "@exemplo.com");
        String convite = convite(paciente, ModoConvite.LINK);
        relogio.avancar(Duration.ofHours(72));
        assertThatThrownBy(() -> cadastroPublicoService.abrir(convite, IP)).isInstanceOf(ConviteInvalidoException.class);
    }

    // --- apoio ---

    private void assertRecusa(String convite, String sessao, FichaCadastroRequest ficha, String trecho) {
        assertThatThrownBy(() -> cadastroPublicoService.revisar(convite, sessao, ficha))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining(trecho);
    }

    private static boolean ehPolitica(FormularioCadastroResponse formulario, UUID termoId) {
        return formulario.termos().stream()
                .anyMatch(t -> t.id().equals(termoId) && t.tipo() == TipoTermo.POLITICA_PRIVACIDADE);
    }

    private static FichaCadastroRequest comDados(FichaCadastroRequest f, DadosCadastroDto dados) {
        return new FichaCadastroRequest(dados, f.modeloAnamneseId(), f.respostas(), f.termos(), f.acompanhante());
    }

    private static FichaCadastroRequest comTermos(FichaCadastroRequest f, List<DecisaoTermoRequest> termos) {
        return new FichaCadastroRequest(f.dados(), f.modeloAnamneseId(), f.respostas(), termos, f.acompanhante());
    }

    /** Responde tudo com a resposta "normal", completa o endereço, aceita a política e recusa o resto. */
    private static FichaCadastroRequest fichaCompleta(FormularioCadastroResponse formulario) {
        Map<String, RespostaAnamneseDto> respostas = new HashMap<>();
        for (PerguntaAnamneseDto p : formulario.anamnese().perguntas()) {
            respostas.put(p.codigo(), switch (p.tipo()) {
                case SIM_NAO, SIM_NAO_DETALHE -> new RespostaAnamneseDto("NAO", null, null);
                case ESCOLHA_UNICA -> new RespostaAnamneseDto(p.opcoes().getFirst(), null, null);
                case MULTIPLA_ESCOLHA -> new RespostaAnamneseDto(null, List.of(p.opcoes().getFirst()), null);
                case DATA -> new RespostaAnamneseDto("2020-01-01", null, null);
                case NUMERO -> new RespostaAnamneseDto("1", null, null);
                case TEXTO_CURTO, TEXTO_LONGO -> new RespostaAnamneseDto("Nada a declarar", null, null);
            });
        }
        DadosCadastroDto d = formulario.dados();
        DadosCadastroDto dados = new DadosCadastroDto(
                d.nomeCompleto(), d.cpf(), d.dataNascimento(), d.email(), d.telefone(),
                new EnderecoDto("45000-000", "Rua das Flores", "10", null, "Centro", "Vitória da Conquista", "BA"),
                d.responsavel());
        List<DecisaoTermoRequest> termos = formulario.termos().stream()
                .map(t -> new DecisaoTermoRequest(t.id(), t.obrigatorio()))
                .toList();
        return new FichaCadastroRequest(dados, formulario.anamnese().id(), respostas, termos, null);
    }

    private String convite(UUID paciente, ModoConvite modo) {
        String link = conviteService.criar(paciente, modo, false, modo == ModoConvite.QUIOSQUE, recepcao).link();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private UUID adulto(String email) {
        return adulto(email, CpfsFicticios.gerar());
    }

    private UUID adulto(String email, String cpf) {
        String nome = Character.toUpperCase(email.charAt(0)) + email.substring(1, email.indexOf('.')) + " Teste "
                + UUID.randomUUID();
        return pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                nome, cpf, LocalDate.of(1990, 3, 15), email, null, null, null, null, false),
                        recepcao)
                .getId();
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil, null,
                null, null, null, "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, relogio.instant()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }

    private int contarAuditoria(String acao, UUID entidadeId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE acao = ? AND entidade_id = ?", Integer.class, acao, entidadeId.toString());
        return total == null ? 0 : total;
    }

    private static String outroCodigo(String codigo) {
        return codigo.equals("000000") ? "111111" : "000000";
    }

    private static String ultimoCodigo(String email) {
        MimeMessage mensagem = Arrays.stream(GREENMAIL.getReceivedMessages())
                .filter(m -> Arrays.stream(destinatarios(m)).anyMatch(a -> a.toString().contains(email)))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow(() -> new AssertionError("Nenhum e-mail para " + email));
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(GreenMailUtil.getBody(mensagem));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static jakarta.mail.Address[] destinatarios(MimeMessage mensagem) {
        try {
            return mensagem.getAllRecipients();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
