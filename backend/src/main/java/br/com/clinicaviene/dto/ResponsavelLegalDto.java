package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.Parentesco;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsável legal informado no cadastro do menor. */
public record ResponsavelLegalDto(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotBlank String cpf,
        @NotNull Parentesco parentesco,
        @Size(max = 20) String telefone,
        @Email @Size(max = 254) String email) {

    @Override
    public String toString() {
        return "ResponsavelLegalDto[parentesco=" + parentesco + "]";
    }
}
