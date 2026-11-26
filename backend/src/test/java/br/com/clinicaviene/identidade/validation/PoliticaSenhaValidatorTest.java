package br.com.clinicaviene.identidade.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** D-066: mínimo 12 caracteres, 1 maiúscula, 1 minúscula, 1 especial. */
public class PoliticaSenhaValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"Clinica#Viene", "abcdefghijK!", "Árvore.secreta", "senha Com espaço!"})
    void aceitaSenhasQueAtendemATodasAsRegras(String senha) {
        assertThat(PoliticaSenhaValidator.violacoes(senha)).isEmpty();
    }

    @Test
    void exigeTamanhoMinimo() {
        assertThat(PoliticaSenhaValidator.violacoes("Curta#1aB")).containsExactly("ter pelo menos 12 caracteres");
    }

    @Test
    void exigeMaiuscula() {
        assertThat(PoliticaSenhaValidator.violacoes("semmaiuscula#123")).containsExactly("ter pelo menos 1 letra maiúscula");
    }

    @Test
    void exigeMinuscula() {
        assertThat(PoliticaSenhaValidator.violacoes("SEMMINUSCULA#123")).containsExactly("ter pelo menos 1 letra minúscula");
    }

    @Test
    void exigeCaractereEspecialEEspacoNaoConta() {
        assertThat(PoliticaSenhaValidator.violacoes("SemEspecial123")).containsExactly("ter pelo menos 1 caractere especial");
        assertThat(PoliticaSenhaValidator.violacoes("Sem Especial 123")).containsExactly("ter pelo menos 1 caractere especial");
    }

    @Test
    void numeroNaoEhObrigatorio() {
        assertThat(PoliticaSenhaValidator.valida("SemNumero#Algum")).isTrue();
    }

    @Test
    void limitaTamanhoMaximoEListaTudoQueFalta() {
        assertThat(PoliticaSenhaValidator.violacoes("A#" + "a".repeat(127))).containsExactly("ter no máximo 128 caracteres");
        assertThat(PoliticaSenhaValidator.violacoes(null)).hasSize(4);
        assertThat(PoliticaSenhaValidator.violacoes("")).hasSize(4);
    }
}
