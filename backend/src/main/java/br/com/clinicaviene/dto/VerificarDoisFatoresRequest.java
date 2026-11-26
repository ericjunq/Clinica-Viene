package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.MetodoDoisFatores;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /api/v1/auth/2fa/verificar}. */
public record VerificarDoisFatoresRequest(
        @NotBlank String desafio, @NotNull MetodoDoisFatores metodo, @NotBlank String codigo) {}
