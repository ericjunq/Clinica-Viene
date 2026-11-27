package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cria ou altera consultório. {@code ativo} ausente = ativo. */
public record ConsultorioRequest(@NotBlank @Size(max = 80) String nome, Boolean ativo) {

    public boolean estaAtivo() {
        return !Boolean.FALSE.equals(ativo);
    }
}
