package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.TipoExcecaoAgenda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Horário extra (brecha fora da rotina) ou ausência: {@code POST /api/v1/profissionais/{id}/excecoes}.
 *
 * @param consultorioId obrigatório no horário extra; na ausência, vazio = todos os consultórios
 */
public record NovaExcecaoAgendaRequest(
        @NotNull TipoExcecaoAgenda tipo,
        UUID consultorioId,
        @NotNull Instant inicio,
        @NotNull Instant fim,
        @Size(max = 300) String motivo) {}
