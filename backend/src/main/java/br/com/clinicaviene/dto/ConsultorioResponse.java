package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Consultorio;
import java.util.UUID;

public record ConsultorioResponse(UUID id, String nome, boolean ativo) {

    public static ConsultorioResponse from(Consultorio c) {
        return new ConsultorioResponse(c.getId(), c.getNome(), c.isAtivo());
    }
}
