package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** O horário fixo vale até {@code ultimoDia} (inclusive). */
public record EncerrarHorarioFixoRequest(@NotNull LocalDate ultimoDia) {}
