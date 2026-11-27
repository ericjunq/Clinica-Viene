package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Nova consulta: {@code POST /api/v1/agendamentos}.
 *
 * @param duracaoMinutos ausente = duração padrão da agenda (30 min)
 */
public record NovoAgendamentoRequest(
        @NotNull UUID pacienteId,
        @NotNull UUID profissionalId,
        @NotNull UUID consultorioId,
        @NotNull Instant inicio,
        @Positive Integer duracaoMinutos,
        @NotEmpty @Size(max = 10) List<@Valid @NotNull ItemAgendamentoRequest> itens,
        @Size(max = 500) String observacao) {}
