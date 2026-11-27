package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdUtilsTest {

    @Test
    void geraUuidVersao7VarianteRfc() {
        UUID id = IdUtils.novoId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void carregaOInstanteEOrdenaPeloTempo() {
        UUID antes = IdUtils.novoId(1_764_000_000_000L);
        UUID depois = IdUtils.novoId(1_764_000_000_001L);

        assertThat(antes.getMostSignificantBits() >>> 16).isEqualTo(1_764_000_000_000L);
        assertThat(antes.toString().compareTo(depois.toString())).isNegative();
    }

    @Test
    void naoRepete() {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(IdUtils.novoId());
        }
        assertThat(ids).hasSize(10_000);
    }
}
