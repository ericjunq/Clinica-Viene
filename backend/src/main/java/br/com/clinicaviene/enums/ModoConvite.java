package br.com.clinicaviene.enums;

/** Onde o paciente vai preencher (D-091). */
public enum ModoConvite {
    /** No próprio celular (link/QR, pode ir por e-mail). Origem registrada: PACIENTE_LINK. */
    LINK,
    /** Num aparelho da clínica, aberto pela recepção, com documento conferido. Origem: PACIENTE_QUIOSQUE. */
    QUIOSQUE;

    public OrigemAlteracao origem() {
        return this == LINK ? OrigemAlteracao.PACIENTE_LINK : OrigemAlteracao.PACIENTE_QUIOSQUE;
    }
}
