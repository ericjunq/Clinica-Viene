package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

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

/** {@code /api/v1/pacientes/{id}/convites} pela API: matriz de autorização e formato das respostas. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConviteControllerIT {

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

    @ParameterizedTest(name = "{0} gera convite → {1}")
    @CsvSource({"ADMIN, 201", "MASTER, 201", "PROFISSIONAL, 403", "DEV, 403"})
    void criar(Perfil perfil, int status) {
        String paciente = novoPaciente();
        comoUsuario(perfil)
                .contentType(ContentType.JSON)
                .body(Map.of("modo", "LINK"))
                .post(convites(paciente))
                .then()
                .statusCode(status);
    }

    @ParameterizedTest(name = "{0} lista convites → {1}")
    @CsvSource({"ADMIN, 200", "MASTER, 200", "PROFISSIONAL, 403", "DEV, 403"})
    void listar(Perfil perfil, int status) {
        comoUsuario(perfil).get(convites(novoPaciente())).then().statusCode(status);
    }

    @Test
    void anonimoNaoAcessa() {
        given().port(porta).get(convites(UUID.randomUUID().toString())).then().statusCode(401);
    }

    @Test
    void linkSoNaCriacaoListaSemTokenECancela() {
        String paciente = novoPaciente();
        RequestSpecification recepcao = comoUsuario(Perfil.ADMIN);

        String conviteId = recepcao
                .contentType(ContentType.JSON)
                .body(Map.of("modo", "LINK"))
                .post(convites(paciente))
                .then()
                .statusCode(201)
                .body("link", startsWith("http://localhost:4200/cadastro#"))
                .body("qrCode", startsWith("data:image/png;base64,"))
                .extract()
                .path("id");

        comoUsuario(Perfil.ADMIN)
                .get(convites(paciente))
                .then()
                .statusCode(200)
                .body("[0].situacao", equalTo("ATIVO"))
                .body("[0]", not(hasKey("link")))
                .body("[0]", not(hasKey("tokenHash")));

        comoUsuario(Perfil.ADMIN).post(convites(paciente) + "/" + conviteId + "/cancelar").then().statusCode(204);
        comoUsuario(Perfil.ADMIN).get(convites(paciente)).then().body("[0].situacao", equalTo("CANCELADO"));
    }

    @Test
    void errosViramProblemDetails() {
        String paciente = novoPaciente();
        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("modo", "QUIOSQUE"))
                .post(convites(paciente))
                .then()
                .statusCode(422);
        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("enviarEmail", true))
                .post(convites(paciente))
                .then()
                .statusCode(400);
        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("modo", "LINK"))
                .post(convites(UUID.randomUUID().toString()))
                .then()
                .statusCode(404);
    }

    // --- apoio ---

    private static String convites(String pacienteId) {
        return "/api/v1/pacientes/" + pacienteId + "/convites";
    }

    private String novoPaciente() {
        return comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "nomeCompleto", "Maria Convite " + UUID.randomUUID(),
                        "cpf", CpfsFicticios.gerar(),
                        "dataNascimento", "1990-05-20",
                        "email", "maria." + UUID.randomUUID() + "@exemplo.com"))
                .post("/api/v1/pacientes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private RequestSpecification comoUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), "cv." + UUID.randomUUID() + "@exemplo.com", null, perfil,
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
