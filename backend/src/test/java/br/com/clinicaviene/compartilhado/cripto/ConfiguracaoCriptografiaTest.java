package br.com.clinicaviene.compartilhado.cripto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfiguracaoCriptografiaTest {

    private static final String V1 = "bD0OJ2DQDhuJo7PkAai3cvLzjA9fxxy9naHXWJ03xQs=";
    private static final String V2 = "ZAYglggI6eGQbS+w/JkBpg0+KjODXI5NP4YHTuMGugw=";

    @Test
    void leVersoesEChaves() {
        Map<String, byte[]> chaves = ConfiguracaoCriptografia.lerChaves(" v1:" + V1 + " , v2:" + V2 + ",");

        assertThat(chaves).containsOnlyKeys("v1", "v2");
        assertThat(chaves.get("v1")).hasSize(32);
    }

    @Test
    void recusaFormatoInvalidoBase64InvalidoEVersaoRepetida() {
        assertThatThrownBy(() -> ConfiguracaoCriptografia.lerChaves(V1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConfiguracaoCriptografia.lerChaves("v1:não-é-base64!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base64");
        assertThatThrownBy(() -> ConfiguracaoCriptografia.lerChaves("v1:" + V1 + ",v1:" + V2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repetida");
    }
}
