package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code PATCH /api/v1/usuarios/{id}/perfil}. O registro profissional é obrigatório se o novo perfil
 * for PROFISSIONAL e é descartado nos demais (D-086).
 */
public record AlterarPerfilRequest(
        @NotNull Perfil perfil,
        AreaAtuacao areaAtuacao,
        ConselhoProfissional conselho,
        @Size(max = 20) String numeroRegistro,
        @Size(max = 2) String ufRegistro) {

    public RegistroProfissionalDto registro() {
        return new RegistroProfissionalDto(areaAtuacao, conselho, numeroRegistro, ufRegistro);
    }
}
