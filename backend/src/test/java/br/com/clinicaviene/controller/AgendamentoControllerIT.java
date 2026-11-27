package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Agendamentos pela API (D-106, D-021): marcação, conflitos, disponibilidade, situação, remarcar e permissões. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgendamentoControllerIT {

    private static final ZoneId BAHIA = ZoneId.of("America/Bahia");
    private static final String API = "/api/v1/agendamentos";

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

    /** Quarta-feira da semana que vem (sempre no futuro). */
    private LocalDate dia;

    private Usuario recepcao;
    private Usuario profissional;
    private String sala;
    private String categoria;
    private String paciente;

    @BeforeEach
    void preparar() {
        dia = LocalDate.now(BAHIA).plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        recepcao = novoUsuario(Perfil.ADMIN, AreaAtuacao.ODONTOLOGIA);
        profissional = novoUsuario(Perfil.PROFISSIONAL, AreaAtuacao.ODONTOLOGIA);
        sala = consultorio();
        horarioFixo(profissional, sala, 8, 18);
        categoria = categoria(AreaAtuacao.ODONTOLOGIA);
        paciente = paciente();
    }

    @Test
    void marcaComItensEAvisoDeFichaPendente() {
        String outraCategoria = categoria(AreaAtuacao.ODONTOLOGIA);
        Map<String, Object> corpo = agendamento(paciente, profissional, sala, hora(9, 0), null);
        corpo.put("itens", List.of(
                Map.of("categoriaId", categoria, "descricao", "Manutenção do aparelho"),
                Map.of("categoriaId", outraCategoria, "descricao", "Limpeza")));
        corpo.put("observacao", "Prefere manhã");

        String id = como(recepcao)
                .contentType(ContentType.JSON)
                .body(corpo)
                .post(API)
                .then()
                .statusCode(201)
                .body("status", equalTo("AGENDADO"))
                .body("fim", equalTo(hora(9, 30).toString()))
                .body("itens", hasSize(2))
                .body("itens[0].descricao", equalTo("Manutenção do aparelho"))
                .body("paciente.fichaPendente", equalTo(true))
                .body("profissional.nome", equalTo(profissional.getNomeCompleto()))
                .extract()
                .path("id");

        // troca os itens (e a observação) sem mudar o horário
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("itens", List.of(Map.of("categoriaId", outraCategoria, "descricao", "Só limpeza"))))
                .put(API + "/" + id + "/itens")
                .then()
                .statusCode(200)
                .body("itens", hasSize(1))
                .body("itens[0].descricao", equalTo("Só limpeza"))
                .body("observacao", equalTo(null));

        como(recepcao).get(API + "?de=" + dia + "&ate=" + dia + "&profissionalId=" + profissional.getId())
                .then()
                .statusCode(200)
                .body("id", hasItem(id));
        // o profissional vê a própria agenda (mesmo sem filtro), mas não a de outro
        como(profissional).get(API + "?de=" + dia + "&ate=" + dia).then().statusCode(200).body("id", hasItem(id));
        Usuario outro = novoUsuario(Perfil.PROFISSIONAL, AreaAtuacao.ODONTOLOGIA);
        como(outro).get(API + "?de=" + dia + "&ate=" + dia).then().statusCode(200).body("id", not(hasItem(id)));
        como(outro).get(API + "/" + id).then().statusCode(403);
        como(outro).get(API + "?de=" + dia + "&ate=" + dia + "&profissionalId=" + profissional.getId()).then().statusCode(403);
        como(novoUsuario(Perfil.DEV, null)).get(API + "?de=" + dia + "&ate=" + dia).then().statusCode(403);
        // profissional não marca consulta
        como(profissional)
                .contentType(ContentType.JSON)
                .body(agendamento(paciente, profissional, sala, hora(11, 0), null))
                .post(API)
                .then()
                .statusCode(403);
    }

    @Test
    void conflitosDeProfissionalEDeConsultorio() {
        marcar(paciente, profissional, sala, hora(10, 0), 60);

        como(recepcao)
                .contentType(ContentType.JSON)
                .body(agendamento(paciente(), profissional, sala, hora(10, 30), null))
                .post(API)
                .then()
                .statusCode(409)
                .body("detail", equalTo("O profissional já tem consulta neste horário."));

        Usuario colega = novoUsuario(Perfil.PROFISSIONAL, AreaAtuacao.ODONTOLOGIA);
        horarioFixo(colega, sala, 8, 18);
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(agendamento(paciente(), colega, sala, hora(10, 45), null))
                .post(API)
                .then()
                .statusCode(409)
                .body("detail", equalTo("O consultório já está ocupado neste horário."));
        // encostado (11h, quando o outro termina) pode
        marcar(paciente(), colega, sala, hora(11, 0), 30);
    }

    @Test
    void duasRecepcionistasNoMesmoHorarioSoUmaConsegue() throws Exception {
        List<Callable<Integer>> tentativas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String outroPaciente = paciente();
            RequestSpecification quem = como(novoUsuario(Perfil.ADMIN, null));
            tentativas.add(() -> quem.contentType(ContentType.JSON)
                    .body(agendamento(outroPaciente, profissional, sala, hora(14, 0), null))
                    .post(API)
                    .then()
                    .extract()
                    .statusCode());
        }
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Integer> status = new ArrayList<>();
        try {
            for (Future<Integer> f : executor.invokeAll(tentativas)) {
                status.add(f.get());
            }
        } finally {
            executor.shutdown();
        }
        assertThat(status).containsOnly(201, 409);
        assertThat(status).filteredOn(s -> s == 201).hasSize(1);
    }

    @Test
    void recusaForaDaDisponibilidadeDaGradeDoPassadoEDeOutraArea() {
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(agendamento(paciente, profissional, sala, hora(17, 45), null))
                .post(API)
                .then()
                .statusCode(422)
                .body("detail", startsWith("O profissional não atende neste consultório nesse horário"));
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(agendamento(paciente, profissional, sala, hora(9, 10), null))
                .post(API)
                .then()
                .statusCode(422)
                .body("detail", startsWith("Início e duração precisam ser de 15 em 15 minutos"));
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(agendamento(paciente, profissional, sala, Instant.now().minusSeconds(3600), null))
                .post(API)
                .then()
                .statusCode(422);
        Map<String, Object> outraArea = agendamento(paciente, profissional, sala, hora(9, 0), null);
        outraArea.put("itens", List.of(Map.of("categoriaId", categoria(AreaAtuacao.PSICOLOGIA))));
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(outraArea)
                .post(API)
                .then()
                .statusCode(422)
                .body("detail", startsWith("A categoria"));
        Map<String, Object> semItens = agendamento(paciente, profissional, sala, hora(9, 0), null);
        semItens.put("itens", List.of());
        como(recepcao).contentType(ContentType.JSON).body(semItens).post(API).then().statusCode(400);
    }

    @Test
    void checkInEAtendimento() {
        String id = marcar(paciente, profissional, sala, hora(8, 0), 30);

        mudar(recepcao, id, "CONFIRMADO").then().statusCode(200).body("status", equalTo("CONFIRMADO"));
        mudar(recepcao, id, "CONCLUIDO").then().statusCode(422);
        mudar(profissional, id, "FALTOU").then().statusCode(403);
        mudar(recepcao, id, "PACIENTE_CHEGOU").then().statusCode(200).body("chegadaEm", not(equalTo(null)));
        mudar(novoUsuario(Perfil.PROFISSIONAL, AreaAtuacao.ODONTOLOGIA), id, "EM_ATENDIMENTO").then().statusCode(403);
        mudar(profissional, id, "EM_ATENDIMENTO").then().statusCode(200);
        mudar(profissional, id, "CONCLUIDO").then().statusCode(200).body("status", equalTo("CONCLUIDO"));
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("motivo", "Teste"))
                .post(API + "/" + id + "/cancelar")
                .then()
                .statusCode(422);
    }

    @Test
    void remarcarLiberaOHorarioAntigoEGuardaOVinculo() {
        String id = marcar(paciente, profissional, sala, hora(15, 0), 60);

        String novo = como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("inicio", hora(15, 30).toString(), "motivo", "Paciente pediu"))
                .post(API + "/" + id + "/remarcar")
                .then()
                .statusCode(201)
                .body("agendamentoOrigemId", equalTo(id))
                .body("fim", equalTo(hora(16, 30).toString()))
                .body("itens", hasSize(1))
                .extract()
                .path("id");
        como(recepcao)
                .get(API + "/" + id)
                .then()
                .body("status", equalTo("CANCELADO"))
                .body("motivoCancelamento", equalTo("Remarcado: Paciente pediu"));
        // 15h–15h30 ficou livre
        marcar(paciente(), profissional, sala, hora(15, 0), 30);

        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("motivo", "Desistiu"))
                .post(API + "/" + novo + "/cancelar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADO"));
    }

    @Test
    void naoTiraHorarioComConsultaMarcada() {
        marcar(paciente, profissional, sala, hora(16, 0), 30);

        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("tipo", "AUSENCIA", "inicio", hora(15, 0).toString(), "fim", hora(17, 0).toString()))
                .post("/api/v1/profissionais/" + profissional.getId() + "/excecoes")
                .then()
                .statusCode(422)
                .body("detail", startsWith("Há 1 consulta(s) marcada(s)"));

        String fixo = como(profissional)
                .get("/api/v1/profissionais/" + profissional.getId() + "/horarios")
                .then()
                .extract()
                .path("fixos[0].id");
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("ultimoDia", LocalDate.now(BAHIA).toString()))
                .post("/api/v1/profissionais/" + profissional.getId() + "/horarios-fixos/" + fixo + "/encerrar")
                .then()
                .statusCode(422);
    }

    // --- apoio ---

    private Instant hora(int h, int m) {
        return dia.atTime(LocalTime.of(h, m)).atZone(BAHIA).toInstant();
    }

    private io.restassured.response.Response mudar(Usuario quem, String id, String status) {
        return como(quem).contentType(ContentType.JSON).body(Map.of("status", status)).post(API + "/" + id + "/status");
    }

    private Map<String, Object> agendamento(String pacienteId, Usuario prof, String consultorio, Instant inicio, Integer minutos) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("pacienteId", pacienteId);
        corpo.put("profissionalId", prof.getId().toString());
        corpo.put("consultorioId", consultorio);
        corpo.put("inicio", inicio.toString());
        if (minutos != null) {
            corpo.put("duracaoMinutos", minutos);
        }
        corpo.put("itens", List.of(Map.of("categoriaId", categoria)));
        return corpo;
    }

    private String marcar(String pacienteId, Usuario prof, String consultorio, Instant inicio, int minutos) {
        return como(recepcao)
                .contentType(ContentType.JSON)
                .body(agendamento(pacienteId, prof, consultorio, inicio, minutos))
                .post(API)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private void horarioFixo(Usuario prof, String consultorio, int de, int ate) {
        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "consultorioId", consultorio, "diaSemana", dia.getDayOfWeek().name(),
                        "horaInicio", String.format("%02d:00", de), "horaFim", String.format("%02d:00", ate),
                        "vigenciaInicio", LocalDate.now(BAHIA).toString()))
                .post("/api/v1/profissionais/" + prof.getId() + "/horarios-fixos")
                .then()
                .statusCode(201);
    }

    private String consultorio() {
        return como(novoUsuario(Perfil.MASTER, null))
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "Sala " + UUID.randomUUID()))
                .post("/api/v1/consultorios")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String categoria(AreaAtuacao area) {
        String sufixo = String.valueOf(System.nanoTime());
        return como(novoUsuario(Perfil.MASTER, null))
                .contentType(ContentType.JSON)
                .body(Map.of("areaAtuacao", area.name(), "codigo", "TESTE_" + sufixo, "nome", "Teste " + sufixo))
                .post("/api/v1/categorias-procedimento")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String paciente() {
        return como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "nomeCompleto", "Paciente Agenda " + UUID.randomUUID(),
                        "cpf", CpfsFicticios.gerar(),
                        "dataNascimento", "1990-05-20",
                        "telefone", "77999990000"))
                .post("/api/v1/pacientes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private Usuario novoUsuario(Perfil perfil, AreaAtuacao area) {
        boolean prof = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil + " " + UUID.randomUUID(), CpfsFicticios.gerar(), "agd." + UUID.randomUUID() + "@exemplo.com",
                null, perfil,
                prof ? area : null,
                prof ? ConselhoProfissional.CRO : null,
                prof ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                prof ? "BA" : null,
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
