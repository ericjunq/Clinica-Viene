package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /api/v1/auth/2fa/cadastro}. */
public record DesafioRequest(@NotBlank String desafio) {}
