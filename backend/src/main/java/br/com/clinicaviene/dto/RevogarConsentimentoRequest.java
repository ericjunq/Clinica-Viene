package br.com.clinicaviene.dto;

import jakarta.validation.constraints.Size;

/** Revogação pedida pelo paciente na recepção. */
public record RevogarConsentimentoRequest(@Size(max = 500) String justificativa) {}
