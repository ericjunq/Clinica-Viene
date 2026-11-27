package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Quem ajudou o paciente a preencher (não lê, sem celular…) — fica registrado (D-088). */
public record AcompanhanteDto(@NotBlank @Size(max = 150) String nome, @Size(max = 300) String motivo) {

    @Override
    public String toString() {
        return "AcompanhanteDto[***]";
    }
}
