package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/v1/usuarios}. */
public record NovoUsuarioRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotBlank String cpf,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 20) String telefone,
        @NotNull Perfil perfil,
        AreaAtuacao areaAtuacao,
        ConselhoProfissional conselho,
        @Size(max = 20) String numeroRegistro,
        @Size(max = 2) String ufRegistro,
        @NotBlank String senhaProvisoria) {

    public NovoUsuarioDto toDto() {
        return new NovoUsuarioDto(
                nomeCompleto, cpf, email, telefone, perfil, areaAtuacao, conselho, numeroRegistro, ufRegistro,
                senhaProvisoria);
    }

    @Override
    public String toString() {
        // Nunca expor CPF ou senha em logs.
        return "NovoUsuarioRequest[perfil=" + perfil + "]";
    }
}
