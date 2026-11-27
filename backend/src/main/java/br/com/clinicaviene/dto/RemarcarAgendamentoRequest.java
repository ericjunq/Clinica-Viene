package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Remarcar = cancelar este agendamento com o motivo e criar outro ligado a ele, com os mesmos itens.
 *
 * @param duracaoMinutos ausente = mantém a duração
 * @param profissionalId ausente = mesmo profissional
 * @param consultorioId ausente = mesmo consultório
 */
public record RemarcarAgendamentoRequest(
        @NotNull Instant inicio,
        @Positive Integer duracaoMinutos,
        UUID profissionalId,
        UUID consultorioId,
        @NotBlank @Size(max = 300) String motivo) {}
