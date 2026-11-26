package br.com.clinicaviene.compartilhado.cripto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CampoCifradoConverterTest {

    private final CampoCifradoConverter converter = new CampoCifradoConverter(new Cifrador(
            Map.of("v1", Base64.getDecoder().decode("bD0OJ2DQDhuJo7PkAai3cvLzjA9fxxy9naHXWJ03xQs=")), "v1"));

    @Test
    void gravaCifradoELeEmClaro() {
        String noBanco = converter.convertToDatabaseColumn("52601815906");

        assertThat(noBanco).startsWith("v1:").doesNotContain("52601815906");
        assertThat(converter.convertToEntityAttribute(noBanco)).isEqualTo("52601815906");
    }

    @Test
    void nuloContinuaNulo() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
