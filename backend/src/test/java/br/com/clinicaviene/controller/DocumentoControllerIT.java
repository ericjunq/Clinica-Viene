package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.FichasDeTeste;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.AssinarFichaRequest;
import br.com.clinicaviene.dto.AssinaturaRequest;
import br.com.clinicaviene.dto.FichaAssinadaResponse;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.service.CadastroPublicoService;
import br.com.clinicaviene.service.ConviteService;
import br.com.clinicaviene.service.ModeloAnamneseService;
import br.com.clinicaviene.service.PacienteService;
import br.com.clinicaviene.service.SessaoService;
import br.com.clinicaviene.service.TermoService;
import br.com.clinicaviene.util.CpfHasher;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@code /api/v1/pacientes/{id}/documentos}: a recepção vê só os metadados da ficha assinada e gera o link de
 * download (QR na tela ou e-mail) — nunca recebe o PDF (D-088, D-094).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocumentoControllerIT {

    private static final String IP = "203.0.113.20";
    private static final String APARELHO = "Tablet da recepção";

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private ConviteService conviteService;

    @Autowired
    private CadastroPublicoService cadastroPublicoService;

    @Autowired
    private TermoService termoService;

    @Autowired
    private ModeloAnamneseService modeloAnamneseService;

    private UsuarioAutenticado recepcao;

    @BeforeEach
    void preparar() {
        recepcao = new UsuarioAutenticado(novoUsuario(Perfil.ADMIN).getId(), Perfil.ADMIN);
        UsuarioAutenticado master = new UsuarioAutenticado(novoUsuario(Perfil.MASTER).getId(), Perfil.MASTER);
        if (termoService.vigente(TipoTermo.POLITICA_PRIVACIDADE).isEmpty()) {
            TermoVersao t = termoService.criarRascunho(TipoTermo.POLITICA_PRIVACIDADE, "Política", "Texto", master);
            termoService.publicar(t.getId(), master);
        }
        if (modeloAnamneseService.vigente(AreaAtuacao.ODONTOLOGIA).isEmpty()) {
            ModeloAnamnese m = modeloAnamneseService.criarRascunho(
                    AreaAtuacao.ODONTOLOGIA,
                    "Ficha de teste",
                    List.of(new PerguntaAnamneseDto(
                            "alergia", null, "Tem alergia?", TipoPergunta.SIM_NAO, true, null, true, null, null)),
                    master);
            modeloAnamneseService.publicar(m.getId(), master);
        }
    }

    @ParameterizedTest(name = "{0} lista documentos → {1}")
    @CsvSource({"ADMIN, 200", "MASTER, 200", "PROFISSIONAL, 403", "DEV, 403"})
    void listarPorPerfil(Perfil perfil, int status) {
        String paciente = pacienteSemEmail(CpfsFicticios.gerar()).toString();
        comoUsuario(perfil).get(documentos(paciente)).then().statusCode(status);
    }

    @ParameterizedTest(name = "{0} gera link de entrega → {1}")
    @CsvSource({"ADMIN, 201", "MASTER, 201", "PROFISSIONAL, 403", "DEV, 403"})
    void gerarEntregaPorPerfil(Perfil perfil, int status) {
        UUID paciente = pacienteSemEmail(CpfsFicticios.gerar());
        UUID documento = assinarNoQuiosque(paciente).documentoId();
        comoUsuario(perfil)
                .post(documentos(paciente.toString()) + "/" + documento + "/entregas")
                .then()
                .statusCode(status);
    }

    @Test
    void semEmailARecepcaoMostraOQrEOPacienteBaixaComOCpf() {
        String cpf = CpfsFicticios.gerar();
        UUID paciente = pacienteSemEmail(cpf);
        FichaAssinadaResponse assinada = assinarNoQuiosque(paciente);
        assertThat(assinada.entregaEnviadaPara()).isNull();

        comoUsuario(Perfil.ADMIN)
                .get(documentos(paciente.toString()))
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(assinada.documentoId().toString()))
                .body("[0].tipo", equalTo("FICHA_CADASTRO"))
                .body("[0].assinadoPor", equalTo("PACIENTE"))
                .body("[0].metodo", equalTo("NOME_DIGITADO"))
                .body("[0]", not(hasKey("chaveArmazenamento")))
                .body("[0]", not(hasKey("sha256")));

        String entregas = documentos(paciente.toString()) + "/" + assinada.documentoId() + "/entregas";
        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("enviarEmail", true))
                .post(entregas)
                .then()
                .statusCode(422)
                .body("detail", startsWith("Paciente sem e-mail cadastrado"));

        String link = comoUsuario(Perfil.ADMIN)
                .post(entregas)
                .then()
                .statusCode(201)
                .body("link", startsWith("http://localhost:4200/documento#"))
                .body("qrCode", startsWith("data:image/png;base64,"))
                .body("emailEnviadoPara", nullValue())
                .extract()
                .path("link");

        byte[] pdf = given().port(porta)
                .header("X-Entrega-Documento", link.substring(link.indexOf('#') + 1))
                .contentType(ContentType.JSON)
                .body(Map.of("cpf", cpf))
                .post("/api/v1/publico/documentos/baixar")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .extract()
                .asByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void comEmailCadastradoDepoisARecepcaoReenviaOLink() throws Exception {
        UUID paciente = pacienteSemEmail(CpfsFicticios.gerar());
        UUID documento = assinarNoQuiosque(paciente).documentoId();
        String email = "jonas." + UUID.randomUUID() + "@exemplo.com";
        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email))
                .patch("/api/v1/pacientes/" + paciente + "/contato")
                .then()
                .statusCode(200);

        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("enviarEmail", true))
                .post(documentos(paciente.toString()) + "/" + documento + "/entregas")
                .then()
                .statusCode(201)
                .body("emailEnviadoPara", equalTo("j***@exemplo.com"));

        MimeMessage recebido = Arrays.stream(GREENMAIL.getReceivedMessages())
                .filter(m -> {
                    try {
                        return Arrays.stream(m.getAllRecipients()).anyMatch(a -> a.toString().contains(email));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .reduce((a, b) -> b)
                .orElseThrow();
        assertThat(recebido.getContent().toString()).contains("/documento#").doesNotContain("Tem alergia");
    }

    @Test
    void documentoDeOutroPacienteNaoAparece() {
        UUID dono = pacienteSemEmail(CpfsFicticios.gerar());
        UUID documento = assinarNoQuiosque(dono).documentoId();
        UUID outro = pacienteSemEmail(CpfsFicticios.gerar());

        comoUsuario(Perfil.ADMIN)
                .post(documentos(outro.toString()) + "/" + documento + "/entregas")
                .then()
                .statusCode(404);
        comoUsuario(Perfil.ADMIN).get(documentos(UUID.randomUUID().toString())).then().statusCode(404);
        given().port(porta).get(documentos(dono.toString())).then().statusCode(401);
    }

    // --- apoio ---

    private static String documentos(String pacienteId) {
        return "/api/v1/pacientes/" + pacienteId + "/documentos";
    }

    private UUID pacienteSemEmail(String cpf) {
        return pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Jonas Quiosque " + UUID.randomUUID(), cpf, LocalDate.of(1979, 3, 9), null,
                                "77999992222", null, null, null, true),
                        recepcao)
                .getId();
    }

    private FichaAssinadaResponse assinarNoQuiosque(UUID paciente) {
        String link = conviteService
                .criar(paciente, ModoConvite.QUIOSQUE, AreaAtuacao.ODONTOLOGIA, false, true, recepcao)
                .link();
        String convite = link.substring(link.lastIndexOf('#') + 1);
        String sessao = cadastroPublicoService.iniciarQuiosque(convite, IP, APARELHO).token();
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        return cadastroPublicoService.assinar(
                convite, sessao,
                new AssinarFichaRequest(
                        FichasDeTeste.completa(formulario, "77999992222"),
                        new AssinaturaRequest(MetodoAssinatura.NOME_DIGITADO, null, formulario.dados().nomeCompleto()),
                        true),
                IP, APARELHO);
    }

    private Usuario novoUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), "doc." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        Instant agora = Instant.now();
        Usuario usuario = new Usuario(dados, cpfHasher.calcular(dados.cpf()), passwordEncoder.encode("x"), null, agora);
        usuario.definirSenha(passwordEncoder.encode("Senha#Provisoria1"), false, agora);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private RequestSpecification comoUsuario(Perfil perfil) {
        return given().port(porta).auth().oauth2(sessaoService.iniciar(novoUsuario(perfil)).tokenAcesso());
    }
}
