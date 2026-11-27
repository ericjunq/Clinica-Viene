package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarAgendamentoRequest(@NotBlank @Size(max = 300) String motivo) {}
