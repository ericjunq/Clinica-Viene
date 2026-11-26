package br.com.clinicaviene.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class CriptografiaConfigTest {

    private static final String V1 = "bD0OJ2DQDhuJo7PkAai3cvLzjA9fxxy9naHXWJ03xQs=";
    private static final String V2 = "ZAYglggI6eGQbS+w/JkBpg0+KjODXI5NP4YHTuMGugw=";

    @Test
    void leVersoesEChaves() {
        Map<String, byte[]> chaves = CriptografiaConfig.lerChaves(" v1:" + V1 + " , v2:" + V2 + ",");

        assertThat(chaves).containsOnlyKeys("v1", "v2");
        assertThat(chaves.get("v1")).hasSize(32);
    }

    @Test
    void recusaFormatoInvalidoBase64InvalidoEVersaoRepetida() {
        assertThatThrownBy(() -> CriptografiaConfig.lerChaves(V1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CriptografiaConfig.lerChaves("v1:não-é-base64!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base64");
        assertThatThrownBy(() -> CriptografiaConfig.lerChaves("v1:" + V1 + ",v1:" + V2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repetida");
    }
}
