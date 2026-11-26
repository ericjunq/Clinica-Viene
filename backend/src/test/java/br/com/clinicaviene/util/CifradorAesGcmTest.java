package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.exceptions.CriptografiaException;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CifradorAesGcmTest {

    // Chaves fictícias, só para testes.
    private static final byte[] CHAVE_V1 = Base64.getDecoder().decode("bD0OJ2DQDhuJo7PkAai3cvLzjA9fxxy9naHXWJ03xQs=");
    private static final byte[] CHAVE_V2 = Base64.getDecoder().decode("ZAYglggI6eGQbS+w/JkBpg0+KjODXI5NP4YHTuMGugw=");

    private final CifradorAesGcm cifrador = new CifradorAesGcm(Map.of("v1", CHAVE_V1), "v1");

    @Test
    void cifraEDecifraDeVolta() {
        String cifrado = cifrador.cifrar("526.018.159-06");

        assertThat(cifrado).startsWith("v1:").doesNotContain("526");
        assertThat(cifrador.decifrar(cifrado)).isEqualTo("526.018.159-06");
    }

    @Test
    void mesmoTextoGeraValoresDiferentes() {
        // IV aleatório: o mesmo CPF não pode virar o mesmo texto cifrado (senão daria para comparar).
        assertThat(cifrador.cifrar("52601815906")).isNotEqualTo(cifrador.cifrar("52601815906"));
    }

    @Test
    void nuloContinuaNulo() {
        assertThat(cifrador.cifrar(null)).isNull();
        assertThat(cifrador.decifrar(null)).isNull();
    }

    @Test
    void rotacaoDeChaveMantemValoresAntigosLegiveis() {
        String cifradoComV1 = cifrador.cifrar("08301661305");
        CifradorAesGcm aposRotacao = new CifradorAesGcm(Map.of("v1", CHAVE_V1, "v2", CHAVE_V2), "v2");

        String cifradoComV2 = aposRotacao.cifrar("08301661305");

        assertThat(cifradoComV2).startsWith("v2:");
        assertThat(aposRotacao.decifrar(cifradoComV1)).isEqualTo("08301661305");
        assertThat(aposRotacao.decifrar(cifradoComV2)).isEqualTo("08301661305");
    }

    @Test
    void rejeitaValorAdulterado() {
        String cifrado = cifrador.cifrar("18609139034");
        byte[] dados = Base64.getDecoder().decode(cifrado.substring(3));
        dados[dados.length - 1] ^= 1;
        String adulterado = "v1:" + Base64.getEncoder().encodeToString(dados);

        assertThatThrownBy(() -> cifrador.decifrar(adulterado)).isInstanceOf(CriptografiaException.class);
    }

    @Test
    void rejeitaTrocaDaVersaoNoPrefixo() {
        // A versão é dado autenticado: trocar o prefixo invalida o valor, mesmo que a chave fosse a mesma.
        CifradorAesGcm duasVersoesMesmaChave = new CifradorAesGcm(Map.of("v1", CHAVE_V1, "v9", CHAVE_V1), "v1");
        String cifrado = duasVersoesMesmaChave.cifrar("18609139034");

        assertThatThrownBy(() -> duasVersoesMesmaChave.decifrar("v9" + cifrado.substring(2)))
                .isInstanceOf(CriptografiaException.class);
    }

    @Test
    void rejeitaVersaoDesconhecidaEFormatoInvalido() {
        String cifrado = cifrador.cifrar("18609139034");

        assertThatThrownBy(() -> cifrador.decifrar("v7" + cifrado.substring(2)))
                .isInstanceOf(CriptografiaException.class)
                .hasMessageContaining("v7");
        assertThatThrownBy(() -> cifrador.decifrar("sem-separador")).isInstanceOf(CriptografiaException.class);
        assertThatThrownBy(() -> cifrador.decifrar("v1:###")).isInstanceOf(CriptografiaException.class);
        assertThatThrownBy(() -> cifrador.decifrar("v1:AAAA")).isInstanceOf(CriptografiaException.class);
    }

    @Test
    void mensagemDeErroNaoVazaOValor() {
        assertThatThrownBy(() -> cifrador.decifrar("v1:52601815906"))
                .isInstanceOf(CriptografiaException.class)
                .hasMessageNotContaining("52601815906");
    }

    @Test
    void exigeChaveDe32BytesEVersaoAtivaConfigurada() {
        assertThatThrownBy(() -> new CifradorAesGcm(Map.of("v1", new byte[16]), "v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new CifradorAesGcm(Map.of("v1", CHAVE_V1), "v2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("v2");
        assertThatThrownBy(() -> new CifradorAesGcm(Map.of(), "v1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CifradorAesGcm(Map.of("v:1", CHAVE_V1), "v:1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
