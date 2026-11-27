package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.clinicaviene.dto.IntervaloDisponivelDto;
import br.com.clinicaviene.entity.DisponibilidadeProfissional;
import br.com.clinicaviene.entity.ExcecaoAgenda;
import br.com.clinicaviene.enums.TipoExcecaoAgenda;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DisponibilidadeCalculoTest {

    private static final ZoneId BAHIA = ZoneId.of("America/Bahia");
    private static final UUID PROFISSIONAL = UUID.randomUUID();
    private static final UUID SALA_1 = UUID.randomUUID();
    private static final UUID SALA_2 = UUID.randomUUID();
    /** Segunda-feira. */
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 30);
    private static final Instant AGORA = Instant.parse("2026-11-27T12:00:00Z");

    private static DisponibilidadeProfissional fixo(UUID sala, DayOfWeek dia, int de, int ate) {
        return new DisponibilidadeProfissional(
                PROFISSIONAL, sala, dia, LocalTime.of(de, 0), LocalTime.of(ate, 0), LocalDate.of(2026, 1, 1), null,
                UUID.randomUUID(), AGORA);
    }

    private static ExcecaoAgenda excecao(TipoExcecaoAgenda tipo, UUID sala, LocalDate dia, int de, int ate) {
        return new ExcecaoAgenda(PROFISSIONAL, sala, tipo, local(dia, de), local(dia, ate), null, UUID.randomUUID(), AGORA);
    }

    private static Instant local(LocalDate dia, int hora) {
        return LocalDateTime.of(dia, LocalTime.of(hora, 0)).atZone(BAHIA).toInstant();
    }

    @Test
    void horarioFixoSemanalEmHoraLocalDaClinica() {
        List<IntervaloDisponivelDto> r = DisponibilidadeCalculo.calcular(
                SEGUNDA, SEGUNDA.plusDays(13), BAHIA, List.of(fixo(SALA_1, DayOfWeek.MONDAY, 10, 15)), List.of());

        assertThat(r).containsExactly(
                new IntervaloDisponivelDto(SALA_1, local(SEGUNDA, 10), local(SEGUNDA, 15)),
                new IntervaloDisponivelDto(SALA_1, local(SEGUNDA.plusDays(7), 10), local(SEGUNDA.plusDays(7), 15)));
        // 10h na Bahia (UTC-3) = 13h UTC
        assertThat(r.getFirst().inicio()).isEqualTo(Instant.parse("2026-11-30T13:00:00Z"));
    }

    @Test
    void horarioExtraSomaEAusenciaDesconta() {
        LocalDate quinta = SEGUNDA.plusDays(3);
        List<IntervaloDisponivelDto> r = DisponibilidadeCalculo.calcular(
                SEGUNDA, SEGUNDA.plusDays(6), BAHIA,
                List.of(fixo(SALA_1, DayOfWeek.MONDAY, 10, 15)),
                List.of(
                        excecao(TipoExcecaoAgenda.HORARIO_EXTRA, SALA_2, quinta, 8, 11),
                        excecao(TipoExcecaoAgenda.AUSENCIA, null, SEGUNDA, 12, 13)));

        assertThat(r).containsExactly(
                new IntervaloDisponivelDto(SALA_1, local(SEGUNDA, 10), local(SEGUNDA, 12)),
                new IntervaloDisponivelDto(SALA_1, local(SEGUNDA, 13), local(SEGUNDA, 15)),
                new IntervaloDisponivelDto(SALA_2, local(quinta, 8), local(quinta, 11)));
        assertThat(DisponibilidadeCalculo.cabe(r, SALA_1, local(SEGUNDA, 10), local(SEGUNDA, 12))).isTrue();
        assertThat(DisponibilidadeCalculo.cabe(r, SALA_1, local(SEGUNDA, 11), local(SEGUNDA, 13))).isFalse();
        assertThat(DisponibilidadeCalculo.cabe(r, SALA_2, local(SEGUNDA, 10), local(SEGUNDA, 11))).isFalse();
    }

    @Test
    void extraEncostadoNoFixoViraUmTrechoSo() {
        List<IntervaloDisponivelDto> r = DisponibilidadeCalculo.calcular(
                SEGUNDA, SEGUNDA, BAHIA,
                List.of(fixo(SALA_1, DayOfWeek.MONDAY, 10, 15)),
                List.of(excecao(TipoExcecaoAgenda.HORARIO_EXTRA, SALA_1, SEGUNDA, 15, 17)));

        assertThat(r).containsExactly(new IntervaloDisponivelDto(SALA_1, local(SEGUNDA, 10), local(SEGUNDA, 17)));
        assertThat(DisponibilidadeCalculo.cabe(r, SALA_1, local(SEGUNDA, 14), local(SEGUNDA, 16))).isTrue();
    }

    @Test
    void ausenciaDeUmConsultorioNaoAfetaOutroEVigenciaEncerradaNaoConta() {
        DisponibilidadeProfissional encerrado = fixo(SALA_2, DayOfWeek.MONDAY, 16, 18);
        encerrado.encerrar(SEGUNDA.minusDays(1));
        ExcecaoAgenda cancelada = excecao(TipoExcecaoAgenda.AUSENCIA, null, SEGUNDA, 10, 15);
        cancelada.cancelar(UUID.randomUUID(), AGORA);

        List<IntervaloDisponivelDto> r = DisponibilidadeCalculo.calcular(
                SEGUNDA, SEGUNDA, BAHIA,
                List.of(fixo(SALA_1, DayOfWeek.MONDAY, 10, 15), encerrado),
                List.of(excecao(TipoExcecaoAgenda.AUSENCIA, SALA_2, SEGUNDA, 10, 15), cancelada));

        assertThat(r).containsExactly(new IntervaloDisponivelDto(SALA_1, local(SEGUNDA, 10), local(SEGUNDA, 15)));
    }

    @Test
    void feriasInteirasZeramADisponibilidade() {
        List<IntervaloDisponivelDto> r = DisponibilidadeCalculo.calcular(
                SEGUNDA, SEGUNDA.plusDays(6), BAHIA,
                List.of(fixo(SALA_1, DayOfWeek.MONDAY, 10, 15), fixo(SALA_1, DayOfWeek.WEDNESDAY, 8, 12)),
                List.of(new ExcecaoAgenda(
                        PROFISSIONAL, null, TipoExcecaoAgenda.AUSENCIA, SEGUNDA.atStartOfDay(BAHIA).toInstant(),
                        SEGUNDA.plusDays(7).atStartOfDay(BAHIA).toInstant(), "Férias", UUID.randomUUID(), AGORA)));

        assertThat(r).isEmpty();
    }
}
