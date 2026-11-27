package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.PacienteVersao;
import br.com.clinicaviene.enums.OrigemAlteracao;
import java.time.Instant;
import java.util.UUID;

/** Uma entrada do histórico do cadastro: o que mudou, por onde, quem e quando (sem os valores). */
public record PacienteVersaoResponse(
        UUID id, String camposAlterados, OrigemAlteracao origem, UUID alteradoPor, Instant criadoEm) {

    public static PacienteVersaoResponse from(PacienteVersao v) {
        return new PacienteVersaoResponse(v.getId(), v.getCamposAlterados(), v.getOrigem(), v.getAlteradoPor(), v.getCriadoEm());
    }
}
