package br.com.clinicaviene.dto;

import java.time.Instant;
import java.util.UUID;

/** Trecho em que o profissional atende num consultório (já somados extras e descontadas ausências). */
public record IntervaloDisponivelDto(UUID consultorioId, Instant inicio, Instant fim) {

    public boolean contem(UUID consultorio, Instant de, Instant ate) {
        return consultorioId.equals(consultorio) && !de.isBefore(inicio) && !ate.isAfter(fim);
    }
}
