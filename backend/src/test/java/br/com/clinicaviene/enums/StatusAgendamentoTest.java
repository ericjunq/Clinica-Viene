package br.com.clinicaviene.enums;

import static br.com.clinicaviene.enums.StatusAgendamento.AGENDADO;
import static br.com.clinicaviene.enums.StatusAgendamento.CANCELADO;
import static br.com.clinicaviene.enums.StatusAgendamento.CONCLUIDO;
import static br.com.clinicaviene.enums.StatusAgendamento.CONFIRMADO;
import static br.com.clinicaviene.enums.StatusAgendamento.EM_ATENDIMENTO;
import static br.com.clinicaviene.enums.StatusAgendamento.FALTOU;
import static br.com.clinicaviene.enums.StatusAgendamento.PACIENTE_CHEGOU;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusAgendamentoTest {

    @Test
    void fluxoNormalECheckInSemConfirmar() {
        assertThat(AGENDADO.podeIrPara(CONFIRMADO)).isTrue();
        assertThat(CONFIRMADO.podeIrPara(PACIENTE_CHEGOU)).isTrue();
        assertThat(AGENDADO.podeIrPara(PACIENTE_CHEGOU)).isTrue();
        assertThat(PACIENTE_CHEGOU.podeIrPara(EM_ATENDIMENTO)).isTrue();
        assertThat(EM_ATENDIMENTO.podeIrPara(CONCLUIDO)).isTrue();
    }

    @Test
    void naoPulaEtapasNemVoltaDeEstadoFinal() {
        assertThat(AGENDADO.podeIrPara(EM_ATENDIMENTO)).isFalse();
        assertThat(AGENDADO.podeIrPara(CONCLUIDO)).isFalse();
        assertThat(EM_ATENDIMENTO.podeIrPara(CANCELADO)).isFalse();
        assertThat(PACIENTE_CHEGOU.podeIrPara(FALTOU)).isFalse();
        for (StatusAgendamento s : StatusAgendamento.values()) {
            assertThat(CONCLUIDO.podeIrPara(s)).isFalse();
            assertThat(FALTOU.podeIrPara(s)).isFalse();
            assertThat(CANCELADO.podeIrPara(s)).isFalse();
        }
    }

    @Test
    void soAgendadoOuConfirmadoEstaoEmAberto() {
        assertThat(AGENDADO.emAberto()).isTrue();
        assertThat(CONFIRMADO.emAberto()).isTrue();
        assertThat(PACIENTE_CHEGOU.emAberto()).isFalse();
    }
}
