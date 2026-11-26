package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LimiteTentativasServiceTest {

    private final LimiteTentativasService limite = new LimiteTentativasService(new AuthProperties(
            "chave",
            Duration.ofMinutes(15),
            Duration.ofHours(12),
            Duration.ofSeconds(20),
            Duration.ofMinutes(5),
            5,
            true,
            3,
            2,
            Duration.ofMinutes(15)));

    @Test
    void bloqueiaOIpDepoisDoLimiteDeTentativas() {
        for (int i = 0; i < 3; i++) {
            limite.verificarLogin("10.0.0.1", null);
        }

        assertThatThrownBy(() -> limite.verificarLogin("10.0.0.1", null))
                .isInstanceOf(MuitasTentativasException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                                ((MuitasTentativasException) e).getSegundosParaTentarDeNovo())
                        .isPositive());
        assertThatCode(() -> limite.verificarLogin("10.0.0.2", null)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaOCpfDepoisDoLimiteDeFalhasVindoDeQualquerIp() {
        limite.registrarFalha("hash-do-cpf");
        assertThatCode(() -> limite.verificarLogin("10.0.0.3", "hash-do-cpf")).doesNotThrowAnyException();
        limite.registrarFalha("hash-do-cpf");

        assertThatThrownBy(() -> limite.verificarLogin("10.0.0.4", "hash-do-cpf"))
                .isInstanceOf(MuitasTentativasException.class);
        assertThatCode(() -> limite.verificarLogin("10.0.0.5", "outro-cpf")).doesNotThrowAnyException();
    }
}
