package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class TotpUtilsTest {

    /** Segredo dos vetores de teste da RFC 6238 (HMAC-SHA1). */
    private static final byte[] SEGREDO_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest(name = "T={0} → {1}")
    @CsvSource({
        "59, 94287082",
        "1111111109, 07081804",
        "1111111111, 14050471",
        "1234567890, 89005924",
        "2000000000, 69279037",
        "20000000000, 65353130"
    })
    void geraOsCodigosDosVetoresDaRfc6238(long segundos, String esperado) {
        long passo = TotpUtils.passo(Instant.ofEpochSecond(segundos));

        assertThat(TotpUtils.codigo(SEGREDO_RFC, passo, 8)).isEqualTo(esperado);
        assertThat(TotpUtils.codigo(SEGREDO_RFC, passo, 6)).isEqualTo(esperado.substring(2));
    }

    @Test
    void base32IdaEVolta() {
        assertThat(TotpUtils.base32(SEGREDO_RFC)).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(TotpUtils.deBase32("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")).isEqualTo(SEGREDO_RFC);
        assertThat(TotpUtils.deBase32("gezd gnbv gy3t qojq gezd gnbv gy3t qojq")).isEqualTo(SEGREDO_RFC);
        assertThatThrownBy(() -> TotpUtils.deBase32("1!")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void segredoNovoTem160BitsEmBase32() {
        String segredo = TotpUtils.gerarSegredo();

        assertThat(segredo).matches("[A-Z2-7]{32}");
        assertThat(TotpUtils.deBase32(segredo)).hasSize(20);
        assertThat(TotpUtils.gerarSegredo()).isNotEqualTo(segredo);
    }

    @Test
    void verificaDentroDaJanelaEDevolveOPassoQueBateu() {
        String segredo = TotpUtils.gerarSegredo();
        long agora = 1_000_000L;

        assertThat(TotpUtils.verificar(segredo, TotpUtils.codigo(segredo, agora), agora, 1)).hasValue(agora);
        assertThat(TotpUtils.verificar(segredo, TotpUtils.codigo(segredo, agora - 1), agora, 1)).hasValue(agora - 1);
        assertThat(TotpUtils.verificar(segredo, TotpUtils.codigo(segredo, agora + 1), agora, 1)).hasValue(agora + 1);
    }

    @Test
    void recusaForaDaJanelaEFormatoInvalido() {
        String segredo = TotpUtils.gerarSegredo();
        long agora = 1_000_000L;
        String antigo = TotpUtils.codigo(segredo, agora - 5);
        boolean coincide = antigo.equals(TotpUtils.codigo(segredo, agora - 1))
                || antigo.equals(TotpUtils.codigo(segredo, agora))
                || antigo.equals(TotpUtils.codigo(segredo, agora + 1));

        if (!coincide) {
            assertThat(TotpUtils.verificar(segredo, antigo, agora, 1)).isEmpty();
        }
        assertThat(TotpUtils.verificar(segredo, "12345", agora, 1)).isEmpty();
        assertThat(TotpUtils.verificar(segredo, "abcdef", agora, 1)).isEmpty();
        assertThat(TotpUtils.verificar(segredo, null, agora, 1)).isEmpty();
    }

    @Test
    void montaAUriDoAppAutenticador() {
        String uri = TotpUtils.uriOtpAuth("ClinicaViene", "ana@exemplo.com", "GEZDGNBVGY3TQOJQ");

        assertThat(uri)
                .startsWith("otpauth://totp/ClinicaViene:ana%40exemplo.com?")
                .contains("secret=GEZDGNBVGY3TQOJQ", "issuer=ClinicaViene", "digits=6", "period=30");
    }
}
