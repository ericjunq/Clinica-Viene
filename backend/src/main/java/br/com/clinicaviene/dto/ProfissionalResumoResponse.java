package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import java.util.UUID;

/** Profissional para a agenda: só nome e área (sem CPF, e-mail ou registro). */
public record ProfissionalResumoResponse(UUID id, String nomeCompleto, AreaAtuacao areaAtuacao) {

    public static ProfissionalResumoResponse from(Usuario u) {
        return new ProfissionalResumoResponse(u.getId(), u.getNomeCompleto(), u.getAreaAtuacao());
    }
}
