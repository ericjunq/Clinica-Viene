package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/v1/usuarios/eu/senha}. */
public record TrocarSenhaRequest(@NotBlank @Size(max = 128) String senhaAtual, @NotBlank @Size(max = 128) String novaSenha) {

    @Override
    public String toString() {
        return "TrocarSenhaRequest[***]";
    }
}
