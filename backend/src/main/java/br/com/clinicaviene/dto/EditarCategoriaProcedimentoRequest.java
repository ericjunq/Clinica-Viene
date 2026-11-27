package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Altera nome e situação (o código não muda). {@code ativa} ausente = ativa. */
public record EditarCategoriaProcedimentoRequest(@NotBlank @Size(max = 80) String nome, Boolean ativa) {

    public boolean estaAtiva() {
        return !Boolean.FALSE.equals(ativa);
    }
}
