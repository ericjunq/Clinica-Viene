package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** Avisos em tempo real por STOMP (D-108): autenticação no CONNECT, aviso de chegada só ao profissional da consulta. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AvisosTempoRealIT {

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

    @Test
    void semTokenNaoConecta() {
        assertThatThrownBy(() -> conectar(null)).isNotNull();
        assertThatThrownBy(() -> conectar("token-falso")).isNotNull();
    }

    @Test
    void checkInAvisaSoOProfissionalDaConsultaEAAgendaAberta() throws Exception {
        Usuario recepcao = novoUsuario(Perfil.ADMIN);
        Usuario profissional = novoUsuario(Perfil.PROFISSIONAL);
        Usuario outroProfissional = novoUsuario(Perfil.PROFISSIONAL);
        LocalDate dia = LocalDate.now(BAHIA).plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        String sala = criar(novoUsuario(Perfil.MASTER), "/api/v1/consultorios", Map.of("nome", "Sala " + UUID.randomUUID()));
        String categoria = criar(novoUsuario(Perfil.MASTER), "/api/v1/categorias-procedimento", Map.of(
                "areaAtuacao", "ODONTOLOGIA", "codigo", "AVISO_" + System.nanoTime(), "nome", "Aviso " + System.nanoTime()));
        criar(recepcao, "/api/v1/profissionais/" + profissional.getId() + "/horarios-fixos", Map.of(
                "consultorioId", sala, "diaSemana", dia.getDayOfWeek().name(), "horaInicio", "08:00", "horaFim", "12:00",
                "vigenciaInicio", LocalDate.now(BAHIA).toString()));
        String paciente = criar(recepcao, "/api/v1/pacientes", Map.of(
                "nomeCompleto", "Paciente Aviso " + UUID.randomUUID(), "cpf", CpfsFicticios.gerar(),
                "dataNascimento", "1990-05-20", "telefone", "77999990000"));

        BlockingQueue<String> avisosDoProfissional = new LinkedBlockingQueue<>();
        BlockingQueue<String> avisosDoOutro = new LinkedBlockingQueue<>();
        BlockingQueue<String> agenda = new LinkedBlockingQueue<>();
        StompSession sessaoProfissional = conectar(token(profissional));
        StompSession sessaoOutro = conectar(token(outroProfissional));
        StompSession sessaoRecepcao = conectar(token(recepcao));
        assinar(sessaoProfissional, "/user/queue/avisos", avisosDoProfissional);
        assinar(sessaoOutro, "/user/queue/avisos", avisosDoOutro);
        assinar(sessaoRecepcao, "/topic/agenda", agenda);
        Thread.sleep(300); // as assinaturas chegam ao broker antes dos avisos

        String agendamento = criar(recepcao, "/api/v1/agendamentos", Map.of(
                "pacienteId", paciente, "profissionalId", profissional.getId().toString(), "consultorioId", sala,
                "inicio", dia.atTime(LocalTime.of(9, 0)).atZone(BAHIA).toInstant().toString(),
                "itens", List.of(Map.of("categoriaId", categoria))));
        assertThat(agenda.poll(5, TimeUnit.SECONDS)).contains("\"dia\":\"" + dia + "\"").doesNotContain("Paciente");

        como(recepcao)
                .contentType(ContentType.JSON)
                .body(Map.of("status", "PACIENTE_CHEGOU"))
                .post("/api/v1/agendamentos/" + agendamento + "/status")
                .then()
                .statusCode(200);

        String aviso = avisosDoProfissional.poll(5, TimeUnit.SECONDS);
        assertThat(aviso)
                .contains("\"tipo\":\"PACIENTE_CHEGOU\"")
                .contains("Paciente Aviso")
                .contains(agendamento);
        assertThat(avisosDoOutro.poll(1, TimeUnit.SECONDS)).isNull();

        sessaoProfissional.disconnect();
        sessaoOutro.disconnect();
        sessaoRecepcao.disconnect();
    }

    // --- apoio ---

    private StompSession conectar(String token) throws Exception {
        WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders conectar = new StompHeaders();
        if (token != null) {
            conectar.add("Authorization", "Bearer " + token);
        }
        return cliente.connectAsync(
                        "ws://localhost:" + porta + "/api/v1/ws", null, conectar, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private static void assinar(StompSession sessao, String destino, BlockingQueue<String> fila) {
        sessao.subscribe(destino, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders cabecalhos) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders cabecalhos, Object corpo) {
                fila.add(new String((byte[]) corpo, StandardCharsets.UTF_8));
            }
        });
    }

    private String criar(Usuario quem, String caminho, Map<String, Object> corpo) {
        return como(quem)
                .contentType(ContentType.JSON)
                .body(corpo)
                .post(caminho)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String token(Usuario usuario) {
        return sessaoService.iniciar(usuario).tokenAcesso();
    }

    private RequestSpecification como(Usuario usuario) {
        return given().port(porta).auth().oauth2(token(usuario));
    }

    private Usuario novoUsuario(Perfil perfil) {
        boolean prof = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil, CpfsFicticios.gerar(), "aviso." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                prof ? AreaAtuacao.ODONTOLOGIA : null,
                prof ? ConselhoProfissional.CRO : null,
                prof ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                prof ? "BA" : null,
                "Senha#Provisoria1");
        Instant agora = Instant.now();
        Usuario usuario = new Usuario(dados, cpfHasher.calcular(dados.cpf()), passwordEncoder.encode("x"), null, agora);
        usuario.definirSenha(passwordEncoder.encode("Senha#Provisoria1"), false, agora);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
