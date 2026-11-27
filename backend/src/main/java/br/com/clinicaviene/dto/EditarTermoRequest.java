package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edição de rascunho de termo: {@code PUT /api/v1/termos/{id}}. */
public record EditarTermoRequest(@NotBlank @Size(max = 150) String titulo, @NotBlank @Size(max = 100_000) String texto) {}
