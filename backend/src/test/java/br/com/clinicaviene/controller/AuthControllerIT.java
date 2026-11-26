package br.com.clinicaviene.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import br.com.clinicaviene.util.TotpUtils;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Login em 2 passos pela API (D-071, D-072), de ponta a ponta. O relógio da aplicação fica parado
 * ({@link RelogioAjustavel}) para os códigos TOTP serem determinísticos; os testes o avançam quando precisam
 * de um código novo.
 */
@Import({TestcontainersConfiguration.class, RelogioAjustavel.Configuracao.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIT {

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final String SENHA = "Senha#Provisoria1";
    private static final String NOVA_SENHA = "Nova#Senha12345";

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void primeiroAcessoCompletoComCadastroDo2faTrocaDeSenhaRenovacaoELogout() {
        String cpf = criarUsuario(Perfil.MASTER);

        // senha errada e CPF inexistente: mesma resposta genérica
        login(cpf, "Errada#Senha123").then().statusCode(401).body("detail", equalTo("CPF ou senha inválidos."));
        login(CpfsFicticios.gerar(), SENHA).then().statusCode(401).body("detail", equalTo("CPF ou senha inválidos."));

        // senha certa → desafio, etapa de cadastro do 2FA
        Response login = login(cpf, SENHA);
        login.then().statusCode(200).body("etapa", equalTo("CADASTRAR_2FA")).body("desafio", notNullValue());
        String desafio = login.path("desafio");

        // QR code e segredo
        Response cadastro = post("/api/v1/auth/2fa/cadastro", Map.of("desafio", desafio));
        cadastro.then().statusCode(200);
        String segredo = cadastro.path("segredo");
        assertThat((String) cadastro.path("qrCode")).startsWith("data:image/png;base64,");
        assertThat((String) cadastro.path("uriOtpAuth")).startsWith("otpauth://totp/ClinicaViene:");

        // código errado → 401; código certo → sessão + cookie + 10 códigos de recuperação
        post("/api/v1/auth/2fa/confirmar", Map.of("desafio", desafio, "codigo", codigoErrado(segredo)))
                .then()
                .statusCode(401)
                .body("detail", equalTo("Código inválido."));
        Response sessao =
                post("/api/v1/auth/2fa/confirmar", Map.of("desafio", desafio, "codigo", codigoAtual(segredo)));
        sessao.then()
                .statusCode(200)
                .body("tokenAcesso", notNullValue())
                .body("codigosRecuperacao", hasSize(10))
                .body("usuario.trocarSenha", equalTo(true))
                .body("usuario.doisFatoresAtivo", equalTo(true));
        assertThat(sessao.getHeader("Set-Cookie"))
                .contains("cv_renovacao=", "HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
        assertThat(sessao.asString()).doesNotContain(sessao.getCookie("cv_renovacao"));
        String token = sessao.path("tokenAcesso");
        String cookie = sessao.getCookie("cv_renovacao");

        // troca de senha pendente: só /auth/eu e a troca de senha
        get("/api/v1/auth/eu", token).then().statusCode(200).body("trocarSenha", equalTo(true));
        get("/api/v1/usuarios", token).then().statusCode(403);
        post("/api/v1/usuarios/eu/senha", Map.of("senhaAtual", SENHA, "novaSenha", NOVA_SENHA), token)
                .then()
                .statusCode(204);
        get("/api/v1/usuarios", token).then().statusCode(200);

        // renovar: novo token e novo cookie; o cookie antigo logo em seguida = corrida entre abas (409)
        Response renovada = renovar(cookie);
        renovada.then().statusCode(200).body("tokenAcesso", notNullValue());
        String cookieNovo = renovada.getCookie("cv_renovacao");
        assertThat(cookieNovo).isNotEqualTo(cookie);
        renovar(cookie).then().statusCode(409);

        // logout: apaga o cookie e a sessão não renova mais
        Response logout = given().port(porta).cookie("cv_renovacao", cookieNovo).post("/api/v1/auth/logout");
        logout.then().statusCode(204);
        assertThat(logout.getHeader("Set-Cookie")).contains("Max-Age=0");
        renovar(cookieNovo).then().statusCode(401);
        given().port(porta).post("/api/v1/auth/renovar").then().statusCode(401);
    }

    @Test
    void acessosSeguintesComCodigoDoAppSemReusoEComCodigoDeRecuperacaoDeUsoUnico() {
        String cpf = criarUsuario(Perfil.ADMIN);
        Cadastro cadastro = cadastrar2fa(cpf);

        // acesso seguinte → etapa de verificação
        String desafio = login(cpf, SENHA).then().statusCode(200).body("etapa", equalTo("VERIFICAR_2FA")).extract().path("desafio");

        // o mesmo código usado no cadastro não vale de novo (anti-replay)
        verificar(desafio, "TOTP", codigoAtual(cadastro.segredo())).then().statusCode(401);
        relogio.avancar(Duration.ofSeconds(30));
        verificar(desafio, "TOTP", codigoAtual(cadastro.segredo())).then().statusCode(200).body("tokenAcesso", notNullValue());

        // código de recuperação: vale uma vez, digitado de qualquer jeito
        String recuperacao = cadastro.codigosRecuperacao().get(0);
        String desafio2 = login(cpf, SENHA).path("desafio");
        verificar(desafio2, "RECUPERACAO", recuperacao.toLowerCase().replace("-", " ")).then().statusCode(200);
        String desafio3 = login(cpf, SENHA).path("desafio");
        verificar(desafio3, "RECUPERACAO", recuperacao).then().statusCode(401);
    }

    @Test
    void desafioMorreDepoisDeCincoCodigosErrados() {
        String cpf = criarUsuario(Perfil.ADMIN);
        Cadastro cadastro = cadastrar2fa(cpf);
        relogio.avancar(Duration.ofSeconds(30));
        String desafio = login(cpf, SENHA).path("desafio");

        for (int i = 0; i < 5; i++) {
            verificar(desafio, "TOTP", codigoErrado(cadastro.segredo())).then().statusCode(401);
        }

        verificar(desafio, "TOTP", codigoAtual(cadastro.segredo()))
                .then()
                .statusCode(401)
                .body("detail", equalTo("Login expirado ou inválido. Entre novamente."));
    }

    @Test
    void desafioExpiraEmCincoMinutos() {
        String cpf = criarUsuario(Perfil.ADMIN);
        String desafio = login(cpf, SENHA).path("desafio");

        relogio.avancar(Duration.ofMinutes(5));

        post("/api/v1/auth/2fa/cadastro", Map.of("desafio", desafio)).then().statusCode(401);
    }

    @Test
    void cincoSenhasErradasBloqueiamOCpfComRetryAfter() {
        String cpf = criarUsuario(Perfil.ADMIN);

        for (int i = 0; i < 5; i++) {
            login(cpf, "Errada#Senha123").then().statusCode(401);
        }

        Response bloqueado = login(cpf, SENHA);
        bloqueado.then().statusCode(429);
        assertThat(Long.parseLong(bloqueado.getHeader("Retry-After"))).isPositive();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit_log WHERE acao = 'LOGIN_BLOQUEADO_TENTATIVAS'", Integer.class))
                .isPositive();
    }

    @Test
    void usuarioDesativadoPerdeOAcessoNaHoraEInativoNaoEntra() {
        String cpf = criarUsuario(Perfil.PROFISSIONAL);
        Cadastro cadastro = cadastrar2fa(cpf);
        get("/api/v1/auth/eu", cadastro.token()).then().statusCode(200);

        jdbc.update("UPDATE usuario SET status = 'INATIVO' WHERE cpf_hash = ?", cpfHasher.calcular(cpf));

        get("/api/v1/auth/eu", cadastro.token()).then().statusCode(401);
        login(cpf, SENHA).then().statusCode(401).body("detail", equalTo("CPF ou senha inválidos."));
    }

    @Test
    void codigo2faPorEmailComoAlternativaAoAppComEsperaParaReenviar() {
        String cpf = criarUsuario(Perfil.ADMIN);
        String email = emailDe(cpf);

        // primeiro acesso: o e-mail não substitui o cadastro do app
        String desafioCadastro = login(cpf, SENHA).path("desafio");
        post("/api/v1/auth/2fa/email", Map.of("desafio", desafioCadastro)).then().statusCode(422);

        cadastrar2fa(cpf);
        String desafio = login(cpf, SENHA).path("desafio");
        post("/api/v1/auth/2fa/email", Map.of("desafio", desafio))
                .then()
                .statusCode(202)
                .body("podeReenviarEm", notNullValue());
        String codigo = ultimoCodigo(email);

        // reenviar logo em seguida: espera de 1 min (429 + Retry-After)
        Response reenvio = post("/api/v1/auth/2fa/email", Map.of("desafio", desafio));
        reenvio.then().statusCode(429);
        assertThat(Long.parseLong(reenvio.getHeader("Retry-After"))).isBetween(1L, 60L);

        verificar(desafio, "EMAIL", codigo).then().statusCode(200).body("tokenAcesso", notNullValue());
    }

    @Test
    void recuperacaoDeSenhaPorEmailSemRevelarCadastroEDerrubandoSessoes() {
        String cpf = criarUsuario(Perfil.ADMIN);
        String email = emailDe(cpf);
        Cadastro cadastro = cadastrar2fa(cpf);
        String cookieAntigo = cadastro.cookie();

        // CPF sem cadastro e CPF cadastrado: mesma resposta
        String mensagemDesconhecido = post("/api/v1/auth/senha/recuperar", Map.of("cpf", CpfsFicticios.gerar()))
                .then()
                .statusCode(202)
                .extract()
                .path("mensagem");
        int emailsAntes = GREENMAIL.getReceivedMessages().length;
        post("/api/v1/auth/senha/recuperar", Map.of("cpf", cpf))
                .then()
                .statusCode(202)
                .body("mensagem", equalTo(mensagemDesconhecido));
        assertThat(GREENMAIL.getReceivedMessages()).hasSize(emailsAntes + 1);
        String codigo = ultimoCodigo(email);

        // senha fraca não gasta o código; código errado → 401
        post("/api/v1/auth/senha/redefinir", Map.of("cpf", cpf, "codigo", codigo, "novaSenha", "fraca"))
                .then()
                .statusCode(422);
        String errado = codigo.equals("000000") ? "111111" : "000000";
        post("/api/v1/auth/senha/redefinir", Map.of("cpf", cpf, "codigo", errado, "novaSenha", NOVA_SENHA))
                .then()
                .statusCode(401);
        post("/api/v1/auth/senha/redefinir", Map.of("cpf", cpf, "codigo", codigo, "novaSenha", NOVA_SENHA))
                .then()
                .statusCode(204);

        // senha antiga não entra; a nova entra e ainda exige o 2FA; sessões antigas caíram
        login(cpf, SENHA).then().statusCode(401);
        login(cpf, NOVA_SENHA).then().statusCode(200).body("etapa", equalTo("VERIFICAR_2FA"));
        renovar(cookieAntigo).then().statusCode(401);
        post("/api/v1/auth/senha/redefinir", Map.of("cpf", cpf, "codigo", codigo, "novaSenha", "Outra#Senha12345"))
                .then()
                .statusCode(401);
    }

    @Test
    void validaOCorpoDaRequisicao() {
        post("/api/v1/auth/login", Map.of("cpf", "")).then().statusCode(400).body("erros", notNullValue());
        post("/api/v1/auth/2fa/verificar", Map.of("desafio", "x", "metodo", "TOTP", "codigo", "123456"))
                .then()
                .statusCode(401);
    }

    // --- apoio ---

    private record Cadastro(String segredo, String token, List<String> codigosRecuperacao, String cookie) {}

    /** Faz o primeiro acesso completo (cadastro do 2FA) e devolve o segredo, o token e os códigos. */
    private Cadastro cadastrar2fa(String cpf) {
        String desafio = login(cpf, SENHA).path("desafio");
        String segredo = post("/api/v1/auth/2fa/cadastro", Map.of("desafio", desafio)).path("segredo");
        Response sessao =
                post("/api/v1/auth/2fa/confirmar", Map.of("desafio", desafio, "codigo", codigoAtual(segredo)));
        sessao.then().statusCode(200);
        return new Cadastro(
                segredo, sessao.path("tokenAcesso"), sessao.path("codigosRecuperacao"), sessao.getCookie("cv_renovacao"));
    }

    private String criarUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa " + perfil,
                CpfsFicticios.gerar(),
                "auth." + UUID.randomUUID() + "@exemplo.com",
                null,
                perfil,
                profissional ? br.com.clinicaviene.enums.AreaAtuacao.ODONTOLOGIA : null,
                profissional ? br.com.clinicaviene.enums.ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                SENHA);
        usuarioRepository.saveAndFlush(new Usuario(
                dados, cpfHasher.calcular(dados.cpf()), passwordEncoder.encode(SENHA), null, relogio.instant()));
        return dados.cpf();
    }

    private String emailDe(String cpf) {
        return usuarioRepository.findByCpfHash(cpfHasher.calcular(cpf)).orElseThrow().getEmail();
    }

    /** Código de 6 dígitos do último e-mail recebido pelo endereço. */
    private String ultimoCodigo(String email) {
        MimeMessage mensagem = Arrays.stream(GREENMAIL.getReceivedMessages())
                .filter(m -> {
                    try {
                        return Arrays.stream(m.getAllRecipients()).anyMatch(a -> a.toString().contains(email));
                    } catch (MessagingException e) {
                        return false;
                    }
                })
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow(() -> new AssertionError("Nenhum e-mail para " + email));
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(GreenMailUtil.getBody(mensagem));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private String codigoAtual(String segredo) {
        return TotpUtils.codigo(segredo, TotpUtils.passo(relogio.instant()));
    }

    /** Um código que não bate em nenhum passo da janela aceita. */
    private String codigoErrado(String segredo) {
        long passo = TotpUtils.passo(relogio.instant());
        List<String> validos = List.of(
                TotpUtils.codigo(segredo, passo - 1), TotpUtils.codigo(segredo, passo), TotpUtils.codigo(segredo, passo + 1));
        for (int candidato = 0; ; candidato++) {
            String codigo = String.format("%06d", candidato);
            if (!validos.contains(codigo)) {
                return codigo;
            }
        }
    }

    private Response login(String cpf, String senha) {
        return post("/api/v1/auth/login", Map.of("cpf", cpf, "senha", senha));
    }

    private Response verificar(String desafio, String metodo, String codigo) {
        return post("/api/v1/auth/2fa/verificar", Map.of("desafio", desafio, "metodo", metodo, "codigo", codigo));
    }

    private Response renovar(String cookie) {
        return given().port(porta).cookie("cv_renovacao", cookie).post("/api/v1/auth/renovar");
    }

    private Response post(String caminho, Map<String, ?> corpo) {
        return given().port(porta).contentType(ContentType.JSON).body(corpo).post(caminho);
    }

    private Response post(String caminho, Map<String, ?> corpo, String token) {
        return given().port(porta)
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .body(corpo)
                .post(caminho);
    }

    private Response get(String caminho, String token) {
        return given().port(porta).auth().oauth2(token).get(caminho);
    }
}
