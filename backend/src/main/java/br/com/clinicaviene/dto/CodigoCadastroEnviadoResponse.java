package br.com.clinicaviene.dto;

import java.time.Instant;

/** Código de confirmação enviado: para onde (mascarado) e quando pode pedir outro (D-073). */
public record CodigoCadastroEnviadoResponse(String emailMascarado, Instant podeReenviarEm, Instant expiraEm) {}
