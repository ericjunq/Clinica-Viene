package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LimiteTentativasServiceTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-11-27T12:00:00Z"));

    private final LimiteTentativasService limite = new LimiteTentativasService(
            new AuthProperties(
                    "chave",
                    Duration.ofMinutes(15),
                    Duration.ofHours(12),
                    Duration.ofSeconds(20),
                    Duration.ofMinutes(5),
                    5,
                    true,
                    3,
                    2,
                    Duration.ofMinutes(15)),
            relogio);

    @Test
    void bloqueiaOIpDepoisDoLimiteDeTentativasELiberaAposAJanela() {
        for (int i = 0; i < 3; i++) {
            limite.verificarLogin("10.0.0.1", null);
        }

        assertThatThrownBy(() -> limite.verificarLogin("10.0.0.1", null))
                .isInstanceOf(MuitasTentativasException.class)
                .satisfies(e -> assertThat(((MuitasTentativasException) e).getSegundosParaTentarDeNovo())
                        .isBetween(1L, 900L));
        assertThatCode(() -> limite.verificarLogin("10.0.0.2", null)).doesNotThrowAnyException();

        relogio.avancar(Duration.ofMinutes(15));
        assertThatCode(() -> limite.verificarLogin("10.0.0.1", null)).doesNotThrowAnyException();
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

    @Test
    void limpezaRemoveSoQuemJaNaoEstaTentando() {
        limite.verificarLogin("10.0.0.6", null);
        limite.registrarFalha("cpf-a");
        assertThat(limite.emAcompanhamento()).isEqualTo(2);

        limite.limparOciosos();
        assertThat(limite.emAcompanhamento()).isEqualTo(2); // tentativas recentes: continuam

        relogio.avancar(Duration.ofMinutes(15));
        limite.limparOciosos();
        assertThat(limite.emAcompanhamento()).isZero(); // janela passou e os baldes encheram: removidos
    }
}
