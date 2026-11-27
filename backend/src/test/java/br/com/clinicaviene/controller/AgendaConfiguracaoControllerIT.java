package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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

/** Consultórios, categorias e horários dos profissionais pela API (D-106): permissões e regras. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgendaConfiguracaoControllerIT {

    private static final ZoneId BAHIA = ZoneId.of("America/Bahia");

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

    @ParameterizedTest(name = "{0} cria consultório → {1}")
    @CsvSource({"MASTER, 201", "ADMIN, 403", "PROFISSIONAL, 403", "DEV, 403"})
    void criarConsultorio(Perfil perfil, int status) {
        como(novoUsuario(perfil))
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "Sala " + UUID.randomUUID()))
                .post("/api/v1/consultorios")
                .then()
                .statusCode(status);
    }

    @ParameterizedTest(name = "{0} lista consultórios → {1}")
    @CsvSource({"MASTER, 200", "ADMIN, 200", "PROFISSIONAL, 200", "DEV, 403"})
    void listarConsultorios(Perfil perfil, int status) {
        como(novoUsuario(perfil)).get("/api/v1/consultorios").then().statusCode(status);
    }

    @Test
    void consultorioComNomeRepetidoEDesativado() {
        String nome = "Sala " + UUID.randomUUID();
        String id = consultorio(nome);
        como(novoUsuario(Perfil.MASTER))
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nome.toUpperCase()))
                .post("/api/v1/consultorios")
                .then()
                .statusCode(422);
        como(novoUsuario(Perfil.MASTER))
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nome, "ativo", false))
                .put("/api/v1/consultorios/" + id)
                .then()
                .statusCode(200)
                .body("ativo", equalTo(false));

        // consultório desativado não recebe horário
        Usuario profissional = novoUsuario(Perfil.PROFISSIONAL);
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(horarioFixo(id, DayOfWeek.MONDAY, "10:00", "15:00"))
                .post(horarios(profissional) + "-fixos")
                .then()
                .statusCode(422)
                .body("detail", startsWith("Consultório não encontrado ou desativado"));
    }

    @Test
    void categoriasDeExemploEGeracaoDoCodigo() {
        Usuario master = novoUsuario(Perfil.MASTER);
        como(master).post("/api/v1/categorias-procedimento/exemplos/odontologia").then().statusCode(200);
        como(master).post("/api/v1/categorias-procedimento/exemplos/odontologia").then().statusCode(200).body("", hasSize(0));
        como(novoUsuario(Perfil.ADMIN))
                .get("/api/v1/categorias-procedimento?area=ODONTOLOGIA")
                .then()
                .statusCode(200)
                .body("codigo", hasItem("ORTODONTIA"))
                .body("codigo", hasItem("CLINICA"));

        String nome = "Avaliação psicológica " + System.nanoTime();
        como(master)
                .contentType(ContentType.JSON)
                .body(Map.of("areaAtuacao", "PSICOLOGIA", "nome", nome))
                .post("/api/v1/categorias-procedimento")
                .then()
                .statusCode(201)
                .body("codigo", startsWith("AVALIACAO_PSICOLOGICA_"));
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of("areaAtuacao", "PSICOLOGIA", "nome", "Outra"))
                .post("/api/v1/categorias-procedimento")
                .then()
                .statusCode(403);
    }

    @Test
    void recepcaoCadastraHorariosEOProfissionalVeOsProprios() {
        String sala1 = consultorio("Sala " + UUID.randomUUID());
        String sala2 = consultorio("Sala " + UUID.randomUUID());
        Usuario profissional = novoUsuario(Perfil.PROFISSIONAL);
        RequestSpecification recepcao = como(novoUsuario(Perfil.ADMIN));
        LocalDate segunda = LocalDate.now(BAHIA).plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate quinta = segunda.plusDays(3);

        // fixo: toda segunda 10h–15h
        recepcao.contentType(ContentType.JSON)
                .body(horarioFixo(sala1, DayOfWeek.MONDAY, "10:00", "15:00"))
                .post(horarios(profissional) + "-fixos")
                .then()
                .statusCode(201);
        // sobreposição no mesmo dia da semana, mesmo em outra sala → 422
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(horarioFixo(sala2, DayOfWeek.MONDAY, "14:00", "16:00"))
                .post(horarios(profissional) + "-fixos")
                .then()
                .statusCode(422);
        // fora da grade de 15 min → 422
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(horarioFixo(sala1, DayOfWeek.TUESDAY, "08:10", "12:00"))
                .post(horarios(profissional) + "-fixos")
                .then()
                .statusCode(422)
                .body("detail", startsWith("Os horários precisam ser de 15 em 15 minutos"));

        // brecha: quinta 8h–11h na sala 2; ausência na segunda 12h–13h
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "tipo", "HORARIO_EXTRA", "consultorioId", sala2,
                        "inicio", local(quinta, 8).toString(), "fim", local(quinta, 11).toString()))
                .post(horarios(profissional).replace("/horarios", "/excecoes"))
                .then()
                .statusCode(201);
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "tipo", "AUSENCIA", "inicio", local(segunda, 12).toString(), "fim", local(segunda, 13).toString(),
                        "motivo", "Consulta médica"))
                .post(horarios(profissional).replace("/horarios", "/excecoes"))
                .then()
                .statusCode(201);
        // horário extra sem consultório → 422
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of("tipo", "HORARIO_EXTRA", "inicio", local(quinta, 17).toString(), "fim", local(quinta, 19).toString()))
                .post(horarios(profissional).replace("/horarios", "/excecoes"))
                .then()
                .statusCode(422);

        String disponibilidade = horarios(profissional).replace("/horarios", "/disponibilidade")
                + "?de=" + segunda + "&ate=" + segunda.plusDays(6);
        como(profissional)
                .get(disponibilidade)
                .then()
                .statusCode(200)
                .body("", hasSize(3))
                .body("[0].inicio", equalTo(local(segunda, 10).toString()))
                .body("[0].fim", equalTo(local(segunda, 12).toString()))
                .body("[1].inicio", equalTo(local(segunda, 13).toString()))
                .body("[2].consultorioId", equalTo(sala2));
        como(profissional)
                .get(horarios(profissional))
                .then()
                .statusCode(200)
                .body("fixos", hasSize(1))
                .body("excecoes", hasSize(2))
                .body("profissional", not(org.hamcrest.Matchers.hasKey("cpf")));

        // outro profissional não vê; DEV não vê
        como(novoUsuario(Perfil.PROFISSIONAL)).get(horarios(profissional)).then().statusCode(403);
        como(novoUsuario(Perfil.DEV)).get(disponibilidade).then().statusCode(403);
        // profissional não cadastra horário
        como(profissional)
                .contentType(ContentType.JSON)
                .body(horarioFixo(sala1, DayOfWeek.FRIDAY, "08:00", "12:00"))
                .post(horarios(profissional) + "-fixos")
                .then()
                .statusCode(403);
    }

    @Test
    void encerrarHorarioFixoECancelarExcecao() {
        String sala = consultorio("Sala " + UUID.randomUUID());
        Usuario profissional = novoUsuario(Perfil.PROFISSIONAL);
        LocalDate hoje = LocalDate.now(BAHIA);
        String fixo = como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(horarioFixo(sala, hoje.getDayOfWeek(), "08:00", "12:00"))
                .post(horarios(profissional) + "-fixos")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        // já em vigor: não dá para encerrar antes do início
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of("ultimoDia", hoje.minusDays(1).toString()))
                .post(horarios(profissional) + "-fixos/" + fixo + "/encerrar")
                .then()
                .statusCode(422);
        como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of("ultimoDia", hoje.toString()))
                .post(horarios(profissional) + "-fixos/" + fixo + "/encerrar")
                .then()
                .statusCode(204);
        como(profissional)
                .get(horarios(profissional).replace("/horarios", "/disponibilidade") + "?de=" + hoje.plusDays(7) + "&ate=" + hoje.plusDays(7))
                .then()
                .statusCode(200)
                .body("", hasSize(0));

        String ausencia = como(novoUsuario(Perfil.ADMIN))
                .contentType(ContentType.JSON)
                .body(Map.of("tipo", "AUSENCIA", "inicio", local(hoje.plusDays(1), 0).toString(), "fim", local(hoje.plusDays(2), 0).toString()))
                .post(horarios(profissional).replace("/horarios", "/excecoes"))
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        como(novoUsuario(Perfil.ADMIN))
                .post(horarios(profissional).replace("/horarios", "/excecoes") + "/" + ausencia + "/cancelar")
                .then()
                .statusCode(204);
        como(profissional).get(horarios(profissional)).then().body("excecoes", hasSize(0));
    }

    @Test
    void listaSoProfissionaisAtivosSemDadosPessoais() {
        Usuario profissional = novoUsuario(Perfil.PROFISSIONAL);
        como(novoUsuario(Perfil.ADMIN))
                .get("/api/v1/profissionais")
                .then()
                .statusCode(200)
                .body("id", hasItem(profissional.getId().toString()))
                .body("[0]", not(org.hamcrest.Matchers.hasKey("email")));
        como(novoUsuario(Perfil.DEV)).get("/api/v1/profissionais").then().statusCode(403);
    }

    // --- apoio ---

    private static Instant local(LocalDate dia, int hora) {
        return dia.atTime(LocalTime.of(hora, 0)).atZone(BAHIA).toInstant();
    }

    private static String horarios(Usuario profissional) {
        return "/api/v1/profissionais/" + profissional.getId() + "/horarios";
    }

    private static Map<String, Object> horarioFixo(String consultorio, DayOfWeek dia, String de, String ate) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("consultorioId", consultorio);
        corpo.put("diaSemana", dia.name());
        corpo.put("horaInicio", de);
        corpo.put("horaFim", ate);
        corpo.put("vigenciaInicio", LocalDate.now(BAHIA).toString());
        return corpo;
    }

    private String consultorio(String nome) {
        return como(novoUsuario(Perfil.MASTER))
                .contentType(ContentType.JSON)
                .body(Map.of("nome", nome))
                .post("/api/v1/consultorios")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private Usuario novoUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), "ag." + UUID.randomUUID() + "@exemplo.com", null, perfil,
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

    private RequestSpecification como(Usuario usuario) {
        return given().port(porta).auth().oauth2(sessaoService.iniciar(usuario).tokenAcesso());
    }
}
