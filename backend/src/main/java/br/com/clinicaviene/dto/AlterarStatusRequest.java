package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.StatusUsuario;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PATCH /api/v1/usuarios/{id}/status}. */
public record AlterarStatusRequest(@NotNull StatusUsuario status) {}
