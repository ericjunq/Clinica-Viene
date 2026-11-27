package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.ExcecaoAgenda;
import br.com.clinicaviene.enums.TipoExcecaoAgenda;
import java.time.Instant;
import java.util.UUID;

public record ExcecaoAgendaResponse(
        UUID id, UUID consultorioId, TipoExcecaoAgenda tipo, Instant inicio, Instant fim, String motivo) {

    public static ExcecaoAgendaResponse from(ExcecaoAgenda e) {
        return new ExcecaoAgendaResponse(
                e.getId(), e.getConsultorioId(), e.getTipo(), e.getInicio(), e.getFim(), e.getMotivo());
    }
}
