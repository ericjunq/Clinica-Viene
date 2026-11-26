package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /api/v1/auth/2fa/confirmar}: o primeiro código gerado pelo app recém-cadastrado. */
public record ConfirmarDoisFatoresRequest(@NotBlank String desafio, @NotBlank String codigo) {}
