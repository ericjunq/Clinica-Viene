package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code PUT /api/v1/usuarios/{id}}: dados editáveis (sem CPF nem perfil — D-086). */
public record AtualizarUsuarioRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 20) String telefone,
        AreaAtuacao areaAtuacao,
        ConselhoProfissional conselho,
        @Size(max = 20) String numeroRegistro,
        @Size(max = 2) String ufRegistro) {

    public RegistroProfissionalDto registro() {
        return new RegistroProfissionalDto(areaAtuacao, conselho, numeroRegistro, ufRegistro);
    }
}
