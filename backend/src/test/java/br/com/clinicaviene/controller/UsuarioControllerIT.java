package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

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
import java.util.HashMap;
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

/** Matriz de autorização de {@code /api/v1/usuarios} pela API: cada perfil × cada endpoint (CONTEXTO §2). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UsuarioControllerIT {

    private static final String SENHA = "Senha#Provisoria1";

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

    @ParameterizedTest(name = "{0} lista usuários → {1}")
    @CsvSource({"DEV, 200", "MASTER, 200", "ADMIN, 403", "PROFISSIONAL, 403"})
    void listar(Perfil perfil, int status) {
        comoUsuario(perfil).get("/api/v1/usuarios").then().statusCode(status);
    }

    @ParameterizedTest(name = "{0} cria ADMIN → {1}")
    @CsvSource({"DEV, 201", "MASTER, 201", "ADMIN, 403", "PROFISSIONAL, 403"})
    void criar(Perfil perfil, int status) {
        comoUsuario(perfil)
                .contentType(ContentType.JSON)
                .body(novoUsuario("ADMIN"))
                .post("/api/v1/usuarios")
                .then()
                .statusCode(status);
    }

    @Test
    void anonimoNaoAcessaNada() {
        given().port(porta).get("/api/v1/usuarios").then().statusCode(401);
        given().port(porta).contentType(ContentType.JSON).body(novoUsuario("ADMIN")).post("/api/v1/usuarios")
                .then()
                .statusCode(401);
        given().port(porta).auth().oauth2("token-falso").get("/api/v1/usuarios").then().statusCode(401);
    }

    @Test
    void criaComCpfMascaradoNaRespostaESemSenha() {
        comoUsuario(Perfil.MASTER)
                .contentType(ContentType.JSON)
                .body(novoUsuario("ADMIN"))
                .post("/api/v1/usuarios")
                .then()
                .statusCode(201)
                .body("cpfMascarado", org.hamcrest.Matchers.matchesPattern("\\*\\*\\*\\.\\d{3}\\.\\d{3}-\\*\\*"))
                .body("trocarSenha", equalTo(true))
                .body("$", not(org.hamcrest.Matchers.hasKey("senhaProvisoria")))
                .body("$", not(org.hamcrest.Matchers.hasKey("cpf")));
    }

    @Test
    void regrasDeNegocioViramProblemDetails() {
        comoUsuario(Perfil.MASTER)
                .contentType(ContentType.JSON)
                .body(novoUsuario("DEV"))
                .post("/api/v1/usuarios")
                .then()
                .statusCode(422)
                .body("detail", equalTo("Usuários DEV não podem ser criados pelo sistema."));

        Map<String, Object> semNome = novoUsuario("ADMIN");
        semNome.remove("nomeCompleto");
        comoUsuario(Perfil.MASTER)
                .contentType(ContentType.JSON)
                .body(semNome)
                .post("/api/v1/usuarios")
                .then()
                .statusCode(400)
                .body("erros.campo", hasItem("nomeCompleto"));
    }

    @Test
    void masterNaoEnxergaDevEDevEnxergaTodos() {
        Usuario dev = criar(Perfil.DEV);

        comoUsuario(Perfil.MASTER).get("/api/v1/usuarios/" + dev.getId()).then().statusCode(404);
        comoUsuario(Perfil.MASTER)
                .get("/api/v1/usuarios")
                .then()
                .statusCode(200)
                .body("perfil", not(hasItem("DEV")));
        comoUsuario(Perfil.DEV).get("/api/v1/usuarios/" + dev.getId()).then().statusCode(200);
    }

    @Test
    void alteraStatusComAsRegrasDePerfil() {
        Usuario admin = criar(Perfil.ADMIN);
        Usuario master = criar(Perfil.MASTER);

        comoUsuario(Perfil.DEV)
                .contentType(ContentType.JSON)
                .body(Map.of("status", "INATIVO"))
                .patch("/api/v1/usuarios/" + master.getId() + "/status")
                .then()
                .statusCode(403);
        comoUsuario(Perfil.MASTER)
                .contentType(ContentType.JSON)
                .body(Map.of("status", "INATIVO"))
                .patch("/api/v1/usuarios/" + admin.getId() + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("INATIVO"))
                .body("id", notNullValue());
    }

    // --- apoio ---

    /** Requisição autenticada como um usuário novo do perfil, com sessão completa (sem troca de senha pendente). */
    private RequestSpecification comoUsuario(Perfil perfil) {
        String token = sessaoService.iniciar(criar(perfil)).tokenAcesso();
        return given().port(porta).auth().oauth2(token);
    }

    private Usuario criar(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil,
                CpfsFicticios.gerar(),
                "matriz." + UUID.randomUUID() + "@exemplo.com",
                null,
                perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                SENHA);
        Instant agora = Instant.now();
        Usuario usuario =
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), passwordEncoder.encode(SENHA), null, agora);
        usuario.definirSenha(passwordEncoder.encode(SENHA), false, agora);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static Map<String, Object> novoUsuario(String perfil) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nomeCompleto", "Nova Pessoa");
        corpo.put("cpf", CpfsFicticios.gerar());
        corpo.put("email", "novo." + UUID.randomUUID() + "@exemplo.com");
        corpo.put("perfil", perfil);
        corpo.put("senhaProvisoria", SENHA);
        return corpo;
    }
}
