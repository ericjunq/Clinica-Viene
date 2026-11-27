package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;

/** Área de atuação + registro no conselho (D-069). */
public record RegistroProfissionalDto(
        AreaAtuacao areaAtuacao, ConselhoProfissional conselho, String numeroRegistro, String ufRegistro) {

    public boolean vazio() {
        return areaAtuacao == null && conselho == null && numeroRegistro == null && ufRegistro == null;
    }

    public boolean completo() {
        return areaAtuacao != null && conselho != null && numeroRegistro != null && ufRegistro != null;
    }
}
