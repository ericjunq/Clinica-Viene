package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Horário fixo semanal (hora local da clínica): {@code POST /api/v1/profissionais/{id}/horarios-fixos}.
 *
 * @param vigenciaFim último dia em que vale; ausente = sem fim
 */
public record NovoHorarioFixoRequest(
        @NotNull UUID consultorioId,
        @NotNull DayOfWeek diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull LocalDate vigenciaInicio,
        LocalDate vigenciaFim) {}
