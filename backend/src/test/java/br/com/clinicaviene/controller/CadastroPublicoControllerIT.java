package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.FichasDeTeste;
import br.com.clinicaviene.dto.AssinarFichaRequest;
import br.com.clinicaviene.dto.AssinaturaRequest;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
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
import br.com.clinicaviene.service.TermoService;
import br.com.clinicaviene.util.CpfHasher;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.restassured.http.ContentType;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

/** {@code /api/v1/publico/cadastro} pela API: sem login, tokens em cabeçalho, respostas de erro. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CadastroPublicoControllerIT {

    private static final String BASE = "/api/v1/publico/cadastro";

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private ConviteService conviteService;

    @Autowired
    private CadastroPublicoService cadastroPublicoService;

    @Autowired
    private JsonMapper jsonMapper;

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

    private UsuarioAutenticado recepcao;

    @BeforeEach
    void preparar() {
        recepcao = usuario(Perfil.ADMIN);
        UsuarioAutenticado master = usuario(Perfil.MASTER);
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

    @Test
    void quiosqueSemLoginAteOFormulario() {
        String convite = convite(ModoConvite.QUIOSQUE);

        given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .get(BASE)
                .then()
                .statusCode(200)
                .body("modo", equalTo("QUIOSQUE"))
                .body("precisaCodigo", equalTo(false))
                .body("primeiroNome", equalTo("Rita"));

        String sessao = given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .post(BASE + "/quiosque")
                .then()
                .statusCode(200)
                .body("expiraEm", notNullValue())
                .extract()
                .path("token");

        given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .header("X-Sessao-Cadastro", sessao)
                .get(BASE + "/formulario")
                .then()
                .statusCode(200)
                .header("Cache-Control", startsWith("no-cache"))
                .body("dados.nomeCompleto", startsWith("Rita"))
                .body("anamnese.perguntas.size()", org.hamcrest.Matchers.greaterThan(0))
                .body("termos.find { it.tipo == 'POLITICA_PRIVACIDADE' }.obrigatorio", equalTo(true));

        given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .header("X-Sessao-Cadastro", sessao)
                .contentType(ContentType.JSON)
                .body(Map.of())
                .post(BASE + "/revisao")
                .then()
                .statusCode(400);
    }

    @Test
    void errosDoFluxoPublico() {
        given().port(porta).get(BASE).then().statusCode(404).body("detail", startsWith("Este link de cadastro é inválido"));
        given().port(porta).header("X-Convite-Cadastro", "falso").post(BASE + "/quiosque").then().statusCode(404);

        String convite = convite(ModoConvite.QUIOSQUE);
        given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .get(BASE + "/formulario")
                .then()
                .statusCode(401);
        given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .header("X-Sessao-Cadastro", "sessao-falsa")
                .get(BASE + "/formulario")
                .then()
                .statusCode(401)
                .body("detail", startsWith("Sua sessão de preenchimento expirou"));

        String link = convite(ModoConvite.LINK);
        given().port(porta)
                .header("X-Convite-Cadastro", link)
                .contentType(ContentType.JSON)
                .body(Map.of("codigo", "000000"))
                .post(BASE + "/codigo/confirmar")
                .then()
                .statusCode(401);
        given().port(porta).header("X-Convite-Cadastro", link).post(BASE + "/quiosque").then().statusCode(422);
    }

    @Test
    void assinaPelaApiEBaixaOPdfComOCpf() throws Exception {
        String email = "rita." + UUID.randomUUID() + "@exemplo.com";
        String cpf = CpfsFicticios.gerar();
        String convite = convite(ModoConvite.QUIOSQUE, email, cpf);
        String sessao = given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .post(BASE + "/quiosque")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        String corpo = jsonMapper.writeValueAsString(new AssinarFichaRequest(
                FichasDeTeste.completa(formulario, null),
                new AssinaturaRequest(MetodoAssinatura.NOME_DIGITADO, null, formulario.dados().nomeCompleto()),
                true));

        given().port(porta)
                .header("X-Convite-Cadastro", convite)
                .header("X-Sessao-Cadastro", sessao)
                .contentType(ContentType.JSON)
                .body(corpo)
                .post(BASE + "/assinatura")
                .then()
                .statusCode(200)
                .body("sha256.length()", equalTo(64))
                .body("entregaEnviadaPara", equalTo("r***@exemplo.com"));

        Matcher m = Pattern.compile("/documento#([A-Za-z0-9_-]+)").matcher(ultimoEmail(email));
        assertThat(m.find()).isTrue();
        String link = m.group(1);
        given().port(porta)
                .header("X-Entrega-Documento", link)
                .contentType(ContentType.JSON)
                .body(Map.of("cpf", CpfsFicticios.gerar()))
                .post("/api/v1/publico/documentos/baixar")
                .then()
                .statusCode(422)
                .body("detail", equalTo("O CPF informado não confere com o cadastro."));
        byte[] pdf = given().port(porta)
                .header("X-Entrega-Documento", link)
                .contentType(ContentType.JSON)
                .body(Map.of("cpf", cpf))
                .post("/api/v1/publico/documentos/baixar")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", startsWith("attachment"))
                .header("Cache-Control", org.hamcrest.Matchers.containsString("no-store"))
                .extract()
                .asByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        given().port(porta)
                .contentType(ContentType.JSON)
                .body(Map.of("cpf", cpf))
                .post("/api/v1/publico/documentos/baixar")
                .then()
                .statusCode(404);
    }

    // --- apoio ---

    private static String ultimoEmail(String email) throws Exception {
        MimeMessage ultima = null;
        for (MimeMessage m : GREENMAIL.getReceivedMessages()) {
            if (java.util.Arrays.stream(m.getAllRecipients()).anyMatch(a -> a.toString().contains(email))) {
                ultima = m;
            }
        }
        assertThat(ultima).as("e-mail para " + email).isNotNull();
        return ultima.getContent().toString();
    }

    private String convite(ModoConvite modo) {
        return convite(modo, "rita." + UUID.randomUUID() + "@exemplo.com", CpfsFicticios.gerar());
    }

    private String convite(ModoConvite modo, String email, String cpf) {
        UUID paciente = pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Rita Publico " + UUID.randomUUID(), cpf, LocalDate.of(1985, 7, 1), email, null, null,
                                null, null, false),
                        recepcao)
                .getId();
        String link = conviteService.criar(paciente, modo, AreaAtuacao.ODONTOLOGIA, false, modo == ModoConvite.QUIOSQUE, recepcao).link();
        return link.substring(link.lastIndexOf('#') + 1);
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil, null,
                null, null, null, "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, Instant.now()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }
}
