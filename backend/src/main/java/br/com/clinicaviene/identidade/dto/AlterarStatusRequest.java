package br.com.clinicaviene.identidade.dto;

import br.com.clinicaviene.identidade.enums.StatusUsuario;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PATCH /api/v1/usuarios/{id}/status}. */
public record AlterarStatusRequest(@NotNull StatusUsuario status) {}
