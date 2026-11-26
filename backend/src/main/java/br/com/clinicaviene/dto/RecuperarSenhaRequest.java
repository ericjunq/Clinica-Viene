package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /api/v1/auth/senha/recuperar}. */
public record RecuperarSenhaRequest(@NotBlank String cpf) {

    @Override
    public String toString() {
        return "RecuperarSenhaRequest[***]";
    }
}
