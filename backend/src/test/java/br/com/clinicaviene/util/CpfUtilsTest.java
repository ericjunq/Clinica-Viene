package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** CPFs fictícios gerados com dígitos verificadores corretos. */
public class CpfUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"526.018.159-06", "52601815906", "083.016.613-05", "186.091.390-34", "529.982.247-25"})
    void aceitaCpfsValidosComOuSemPontuacao(String cpf) {
        assertThat(CpfUtils.valido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "526.018.159-07", // segundo dígito errado
                "526.018.159-16", // primeiro dígito errado
                "111.111.111-11", // todos iguais
                "000.000.000-00",
                "5260181590", // 10 dígitos
                "526018159066", // 12 dígitos
                "abc"
            })
    @NullAndEmptySource
    void recusaCpfsInvalidos(String cpf) {
        assertThat(CpfUtils.valido(cpf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"526.018.159-06", "526 018 159 06", " 52601815906 ", "526-018-159.06"})
    void normalizaParaSoDigitos(String cpf) {
        assertThat(CpfUtils.normalizar(cpf)).isEqualTo("52601815906");
    }
}
