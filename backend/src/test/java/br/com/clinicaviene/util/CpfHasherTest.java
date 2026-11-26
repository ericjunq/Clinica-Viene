package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

public class CpfHasherTest {

    // Chaves fictícias, só para testes.
    private final CpfHasher hash =
            new CpfHasher(Base64.getDecoder().decode("EGXBpRu/5d1AeSsVzXXIPuxpJ55IbTlqQhkebzwDG+s="));
    private final CpfHasher hashComOutraChave =
            new CpfHasher(Base64.getDecoder().decode("G6dZ5LwscTt8QqJ+LCs7lTjYjuXIdZlN81YKn+nIXIs="));

    @Test
    void mesmoCpfComOuSemPontuacaoGeraOMesmoHash() {
        assertThat(hash.calcular("526.018.159-06")).isEqualTo(hash.calcular("52601815906"));
    }

    @Test
    void hashTem64CaracteresHexadecimaisESemOCpf() {
        assertThat(hash.calcular("52601815906")).hasSize(64).matches("[0-9a-f]{64}").doesNotContain("52601815906");
    }

    @Test
    void cpfsDiferentesGeramHashesDiferentes() {
        assertThat(hash.calcular("52601815906")).isNotEqualTo(hash.calcular("08301661305"));
    }

    @Test
    void chaveDiferenteGeraHashDiferente() {
        // Sem a chave secreta não dá para recalcular o hash por força bruta.
        assertThat(hash.calcular("52601815906")).isNotEqualTo(hashComOutraChave.calcular("52601815906"));
    }

    @Test
    void recusaCpfVazioEChaveCurta() {
        assertThatThrownBy(() -> hash.calcular("...-")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hash.calcular(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CpfHasher(new byte[16])).isInstanceOf(IllegalArgumentException.class);
    }
}
