package br.com.clinicaviene.dto;

import java.time.Instant;

/** Código enviado; {@code podeReenviarEm} permite ao frontend mostrar a contagem regressiva (D-073). */
public record EnvioCodigoResponse(Instant podeReenviarEm) {}
