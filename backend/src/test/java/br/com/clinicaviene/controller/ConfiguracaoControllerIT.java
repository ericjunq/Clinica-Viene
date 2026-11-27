package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.service.SessaoService;
import br.com.clinicaviene.util.CpfHasher;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Termos, modelos de anamnese e consentimentos pela API: só o MASTER configura (D-089). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConfiguracaoControllerIT {

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

    @ParameterizedTest(name = "{0} lista termos → {1}")
    @CsvSource({"MASTER, 200", "ADMIN, 403", "PROFISSIONAL, 403", "DEV, 403"})
    void listarTermos(Perfil perfil, int status) {
        comoUsuario(perfil).get("/api/v1/termos").then().statusCode(status);
    }

    @ParameterizedTest(name = "{0} lista modelos de anamnese → {1}")
    @CsvSource({"MASTER, 200", "ADMIN, 403", "PROFISSIONAL, 403", "DEV, 403"})
    void listarModelos(Perfil perfil, int status) {
        comoUsuario(perfil).get("/api/v1/modelos-anamnese").then().statusCode(status);
    }

    @Test
    void masterCriaEditaEPublicaModeloPelaApi() {
        RequestSpecification master = comoUsuario(Perfil.MASTER);
        Map<String, Object> pergunta = Map.of(
                "codigo", "queixa", "secao", "Geral", "texto", "Qual a queixa?", "tipo", "TEXTO_LONGO", "obrigatoria", true);

        String id = master.contentType(ContentType.JSON)
                .body(Map.of("areaAtuacao", "NUTRICAO", "titulo", "Anamnese nutricional", "perguntas", List.of(pergunta)))
                .post("/api/v1/modelos-anamnese")
                .then()
                .statusCode(201)
                .body("status", equalTo("RASCUNHO"))
                .body("perguntas", hasSize(1))
                .extract()
                .path("id");

        comoUsuario(Perfil.MASTER).post("/api/v1/modelos-anamnese/" + id + "/publicar").then().statusCode(200).body("status", equalTo("PUBLICADO"));
        comoUsuario(Perfil.MASTER)
                .contentType(ContentType.JSON)
                .body(Map.of("titulo", "Outro", "perguntas", List.of(pergunta)))
                .put("/api/v1/modelos-anamnese/" + id)
                .then()
                .statusCode(422);
    }

    @Test
    void masterCarregaOsExemplosDeTermosEAnamnese() {
        comoUsuario(Perfil.MASTER).post("/api/v1/termos/exemplos").then().statusCode(200);
        comoUsuario(Perfil.MASTER).get("/api/v1/termos").then().statusCode(200).body("size()", greaterThanOrEqualTo(5));
        comoUsuario(Perfil.MASTER)
                .post("/api/v1/modelos-anamnese/exemplos/odontologia")
                .then()
                .statusCode(201)
                .body("perguntas.size()", greaterThanOrEqualTo(40));
    }

    @Test
    void campoBooleanoAusenteNaPerguntaNaoQuebraOJson() {
        // "obrigatoria", "alertaClinico" etc. ausentes: aceitos (wrappers — regra do Jackson 3)
        Map<String, Object> pergunta = Map.of("codigo", "obs", "texto", "Observações", "tipo", "TEXTO_LONGO");
        comoUsuario(Perfil.MASTER)
                .contentType(ContentType.JSON)
                .body(Map.of("areaAtuacao", "FISIOTERAPIA", "titulo", "Fisio", "perguntas", List.of(pergunta)))
                .post("/api/v1/modelos-anamnese")
                .then()
                .statusCode(201);
    }

    private RequestSpecification comoUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), "cfg." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        Instant agora = Instant.now();
        Usuario usuario = new Usuario(dados, cpfHasher.calcular(dados.cpf()), passwordEncoder.encode("x"), null, agora);
        usuario.definirSenha(passwordEncoder.encode("Senha#Provisoria1"), false, agora);
        usuarioRepository.saveAndFlush(usuario);
        return given().port(porta).auth().oauth2(sessaoService.iniciar(usuario).tokenAcesso());
    }
}
