package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenUtilsTest {

    @Test
    void geraTokensDe256BitsDiferentes() {
        String token = TokenUtils.gerar();

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(TokenUtils.gerar()).isNotEqualTo(token);
    }

    @Test
    void sha256EmHexadecimal() {
        assertThat(TokenUtils.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
