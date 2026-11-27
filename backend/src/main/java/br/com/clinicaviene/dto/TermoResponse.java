package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.enums.TipoTermo;
import java.time.Instant;
import java.util.UUID;

public record TermoResponse(
        UUID id,
        TipoTermo tipo,
        boolean obrigatorio,
        int versao,
        String titulo,
        String texto,
        StatusVersao status,
        Instant criadoEm,
        Instant publicadoEm) {

    public static TermoResponse from(TermoVersao t) {
        return new TermoResponse(
                t.getId(), t.getTipo(), t.getTipo().obrigatorio(), t.getVersao(), t.getTitulo(), t.getTexto(),
                t.getStatus(), t.getCriadoEm(), t.getPublicadoEm());
    }
}
