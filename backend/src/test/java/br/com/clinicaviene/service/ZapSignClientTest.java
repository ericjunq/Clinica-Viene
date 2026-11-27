package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.config.ZapSignProperties;
import br.com.clinicaviene.dto.ZapSignDocumentoResponse;
import br.com.clinicaviene.dto.ZapSignSignatarioRequest;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.exceptions.ZapSignException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fala com um servidor HTTP local que imita a ZapSign: nenhum teste chama a ZapSign de verdade (D-103). */
class ZapSignClientTest {

    private static final String RESPOSTA = """
            {"open_id": 1, "token": "doc-123", "status": "pending", "external_id": "id-local",
             "original_file": "https://exemplo.invalid/original.pdf", "signed_file": null, "campo_novo": true,
             "signers": [{"token": "sig-1", "status": "new", "name": "Fulano de Tal",
                          "sign_url": "https://app.zapsign.com.br/verificar/sig-1", "external_id": "pac-1"}]}
            """;

    private HttpServer servidor;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> autorizacaoRecebida = new AtomicReference<>();
    private final AtomicReference<String> caminhoRecebido = new AtomicReference<>();
    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile int statusResposta = 200;

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/", troca -> {
            chamadas.incrementAndGet();
            caminhoRecebido.set(troca.getRequestMethod() + " " + troca.getRequestURI().getPath());
            autorizacaoRecebida.set(troca.getRequestHeaders().getFirst("Authorization"));
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = (statusResposta == 200 ? RESPOSTA : "{\"detail\":\"erro\"}").getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(statusResposta, resposta.length);
            troca.getResponseBody().write(resposta);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private ZapSignClient cliente(boolean habilitado, String token, DataSize maximo) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/v1";
        return new ZapSignClient(new ZapSignProperties(habilitado, url, token, maximo, Duration.ofSeconds(5)));
    }

    @Test
    void criaDocumentoComPdfEmBase64SemEnvioAutomaticoENomeTravado() {
        byte[] pdf = "%PDF-1.7 conteudo".getBytes(StandardCharsets.US_ASCII);

        ZapSignDocumentoResponse documento = cliente(true, "token-teste", DataSize.ofMegabytes(10))
                .criarDocumento("Ficha de cadastro", pdf, "id-local",
                        List.of(ZapSignSignatarioRequest.assinaturaNaTela("Fulano de Tal", "pac-1")));

        assertThat(caminhoRecebido.get()).isEqualTo("POST /api/v1/docs/");
        assertThat(autorizacaoRecebida.get()).isEqualTo("Bearer token-teste");
        JsonNode corpo = JsonMapper.builder().build().readTree(corpoRecebido.get());
        assertThat(corpo.get("name").asString()).isEqualTo("Ficha de cadastro");
        assertThat(Base64.getDecoder().decode(corpo.get("base64_pdf").asString())).isEqualTo(pdf);
        assertThat(corpo.get("lang").asString()).isEqualTo("pt-br");
        assertThat(corpo.get("external_id").asString()).isEqualTo("id-local");
        assertThat(corpo.get("disable_signer_emails").asBoolean()).isTrue();
        JsonNode signatario = corpo.get("signers").get(0);
        assertThat(signatario.get("auth_mode").asString()).isEqualTo("assinaturaTela");
        assertThat(signatario.get("send_automatic_email").asBoolean()).isFalse();
        assertThat(signatario.get("send_automatic_whatsapp").asBoolean()).isFalse();
        assertThat(signatario.get("lock_name").asBoolean()).isTrue();
        assertThat(signatario.has("email")).isFalse();

        assertThat(documento.token()).isEqualTo("doc-123");
        assertThat(documento.signers()).singleElement().satisfies(s -> {
            assertThat(s.token()).isEqualTo("sig-1");
            assertThat(s.signUrl()).isEqualTo("https://app.zapsign.com.br/verificar/sig-1");
        });
    }

    @Test
    void detalhaDocumentoPeloToken() {
        ZapSignDocumentoResponse documento =
                cliente(true, "token-teste", DataSize.ofMegabytes(10)).detalharDocumento("doc-123");

        assertThat(caminhoRecebido.get()).isEqualTo("GET /api/v1/docs/doc-123/");
        assertThat(documento.status()).isEqualTo("pending");
    }

    @Test
    void desligadoOuSemTokenNaoFazChamada() {
        byte[] pdf = {1, 2, 3};
        var signatarios = List.of(ZapSignSignatarioRequest.assinaturaNaTela("Fulano", "p"));

        assertThatThrownBy(() -> cliente(false, "token", DataSize.ofMegabytes(10))
                        .criarDocumento("Doc", pdf, "id", signatarios))
                .isInstanceOf(ZapSignException.class)
                .hasMessageContaining("desligada");
        assertThatThrownBy(() -> cliente(true, " ", DataSize.ofMegabytes(10)).detalharDocumento("doc"))
                .isInstanceOf(ZapSignException.class)
                .hasMessageContaining("Token");
        assertThat(chamadas).hasValue(0);
    }

    @Test
    void pdfAcimaDoLimiteNaoEEnviado() {
        byte[] pdf = new byte[2048];

        assertThatThrownBy(() -> cliente(true, "token", DataSize.ofKilobytes(1))
                        .criarDocumento("Doc", pdf, "id",
                                List.of(ZapSignSignatarioRequest.assinaturaNaTela("Fulano", "p"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("limite");
        assertThat(chamadas).hasValue(0);
    }

    @Test
    void erroDaZapSignViraZapSignException() {
        statusResposta = 401;

        assertThatThrownBy(() -> cliente(true, "token-errado", DataSize.ofMegabytes(10)).detalharDocumento("doc"))
                .isInstanceOf(ZapSignException.class)
                .hasMessageContaining("Falha na comunicação");
    }
}
