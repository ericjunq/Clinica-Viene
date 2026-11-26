package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/v1/auth/senha/redefinir}. */
public record RedefinirSenhaRequest(
        @NotBlank String cpf, @NotBlank String codigo, @NotBlank @Size(max = 128) String novaSenha) {

    @Override
    public String toString() {
        return "RedefinirSenhaRequest[***]";
    }
}
