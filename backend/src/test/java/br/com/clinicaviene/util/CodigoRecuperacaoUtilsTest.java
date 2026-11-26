package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodigoRecuperacaoUtilsTest {

    @Test
    void geraNoFormatoSemCaracteresAmbiguos() {
        String codigo = CodigoRecuperacaoUtils.gerar();

        assertThat(codigo).matches("[A-HJKMNP-Z2-9]{4}(-[A-HJKMNP-Z2-9]{4}){3}");
    }

    @Test
    void codigosNaoSeRepetem() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codigos.add(CodigoRecuperacaoUtils.gerar());
        }
        assertThat(codigos).hasSize(1000);
    }

    @Test
    void normalizaIgnorandoHifenEspacoEMinusculas() {
        assertThat(CodigoRecuperacaoUtils.normalizar(" abcd-efgh ijkm-npqr "))
                .isEqualTo(CodigoRecuperacaoUtils.normalizar("ABCDEFGHIJKMNPQR"))
                .isEqualTo("ABCDEFGHIJKMNPQR");
        assertThat(CodigoRecuperacaoUtils.normalizar(null)).isEmpty();
    }
}
