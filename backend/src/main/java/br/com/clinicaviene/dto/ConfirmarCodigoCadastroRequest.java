package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarCodigoCadastroRequest(@NotBlank @Size(max = 10) String codigo) {

    @Override
    public String toString() {
        return "ConfirmarCodigoCadastroRequest[***]";
    }
}
