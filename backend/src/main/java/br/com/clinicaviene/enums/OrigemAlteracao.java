package br.com.clinicaviene.enums;

/** Quem/por onde um dado do paciente foi informado ou alterado (CONTEXTO §4.1, D-091). */
public enum OrigemAlteracao {
    /** Recepção, no sistema interno (pré-cadastro, contato, endereço). */
    RECEPCAO,
    /** O próprio paciente, no celular dele (link/QR). */
    PACIENTE_LINK,
    /** O próprio paciente, num aparelho da clínica aberto pela recepção. */
    PACIENTE_QUIOSQUE
}
