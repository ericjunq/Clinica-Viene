package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/v1/auth/login}. */
public record LoginRequest(@NotBlank String cpf, @NotBlank @Size(max = 128) String senha) {

    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}
