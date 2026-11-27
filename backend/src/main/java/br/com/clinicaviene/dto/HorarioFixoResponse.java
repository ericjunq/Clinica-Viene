package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.DisponibilidadeProfissional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record HorarioFixoResponse(
        UUID id,
        UUID consultorioId,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim) {

    public static HorarioFixoResponse from(DisponibilidadeProfissional d) {
        return new HorarioFixoResponse(
                d.getId(), d.getConsultorioId(), d.dia(), d.getHoraInicio(), d.getHoraFim(), d.getVigenciaInicio(),
                d.getVigenciaFim());
    }
}
