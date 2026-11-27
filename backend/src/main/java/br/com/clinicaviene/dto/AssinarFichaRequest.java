package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Ficha completa + assinatura (2F).
 *
 * @param declaracao "declaro que as informações são verdadeiras" — obrigatória
 */
public record AssinarFichaRequest(
        @NotNull @Valid FichaCadastroRequest ficha, @NotNull @Valid AssinaturaRequest assinatura, Boolean declaracao) {

    public boolean declarou() {
        return Boolean.TRUE.equals(declaracao);
    }

    @Override
    public String toString() {
        return "AssinarFichaRequest[***]";
    }
}
