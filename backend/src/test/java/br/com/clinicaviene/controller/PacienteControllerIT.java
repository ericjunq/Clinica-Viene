package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

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
import java.time.LocalDate;
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

/** {@code /api/v1/pacientes} pela API: matriz de autorização, CPF mascarado, busca, duplicidade. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PacienteControllerIT {

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

    @ParameterizedTest(name = "{0} pré-cadastra paciente → {1}")
    @CsvSource({"ADMIN, 201", "MASTER, 201", "PROFISSIONAL, 403", "DEV, 403"})
    void preCadastrar(Perfil perfil, int status) {
        comoUsuario(perfil).contentType(ContentType.JSON).body(adulto()).post("/api/v1/pacientes").then().statusCode(status);
    }

    @ParameterizedTest(name = "{0} pesquisa pacientes → {1}")
    @CsvSource({"ADMIN, 200", "MASTER, 200", "PROFISSIONAL, 403", "DEV, 403"})
    void pesquisar(Perfil perfil, int status) {
        comoUsuario(perfil).get("/api/v1/pacientes?busca=Maria").then().statusCode(status);
    }

    @Test
    void anonimoNaoAcessa() {
        given().port(porta).get("/api/v1/pacientes?busca=x").then().statusCode(401);
    }

    @Test
    void respostaComCpfMascaradoBuscaPorCpfEHistorico() {
        RequestSpecification recepcao = comoUsuario(Perfil.ADMIN);
        Map<String, Object> corpo = adulto();
        String cpf = (String) corpo.get("cpf");

        String id = recepcao.contentType(ContentType.JSON)
                .body(corpo)
                .post("/api/v1/pacientes")
                .then()
                .statusCode(201)
                .body("cpfMascarado", matchesPattern("\\*\\*\\*\\.\\d{3}\\.\\d{3}-\\*\\*"))
                .body("$", not(hasKey("cpf")))
                .body("menorDeIdade", equalTo(false))
                .extract()
                .path("id");

        comoUsuario(Perfil.MASTER).get("/api/v1/pacientes?busca=" + cpf).then().statusCode(200).body("id", hasItem(id));
        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "outro@exemplo.com", "telefone", "77988887777"))
                .patch("/api/v1/pacientes/" + id + "/contato")
                .then()
                .statusCode(200)
                .body("email", equalTo("outro@exemplo.com"));
        comoUsuario(Perfil.ADMIN)
                .get("/api/v1/pacientes/" + id + "/historico")
                .then()
                .statusCode(200)
                .body("camposAlterados", hasItem("e-mail, telefone"))
                .body("[0]", not(hasKey("dados")));
    }

    @Test
    void possivelDuplicidadeResponde409ComOsSemelhantes() {
        Map<String, Object> crianca = new HashMap<>();
        crianca.put("nomeCompleto", "Duplicado " + UUID.randomUUID());
        crianca.put("dataNascimento", LocalDate.now().minusYears(6).toString());
        crianca.put("telefone", "77999990000");
        crianca.put("responsavel", Map.of("nomeCompleto", "Pai Teste", "cpf", CpfsFicticios.gerar(), "parentesco", "PAI"));
        comoUsuario(Perfil.ADMIN).contentType(ContentType.JSON).body(crianca).post("/api/v1/pacientes").then().statusCode(201);

        comoUsuario(Perfil.ADMIN)
                .contentType(ContentType.JSON)
                .body(crianca)
                .post("/api/v1/pacientes")
                .then()
                .statusCode(409)
                .body("pacientesSemelhantes.size()", equalTo(1));
    }

    // --- apoio ---

    private RequestSpecification comoUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), "pc." + UUID.randomUUID() + "@exemplo.com", null, perfil,
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

    private static Map<String, Object> adulto() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nomeCompleto", "Maria Teste");
        corpo.put("cpf", CpfsFicticios.gerar());
        corpo.put("dataNascimento", "1990-05-20");
        corpo.put("email", "maria." + UUID.randomUUID() + "@exemplo.com");
        return corpo;
    }
}
