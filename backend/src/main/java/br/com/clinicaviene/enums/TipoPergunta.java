package br.com.clinicaviene.enums;

/** Tipos de pergunta da anamnese (D-089). */
public enum TipoPergunta {
    /** Sim/Não. */
    SIM_NAO,
    /** Sim/Não com campo "qual?" (detalhe obrigatório quando a resposta é Sim, se configurado). */
    SIM_NAO_DETALHE,
    TEXTO_CURTO,
    TEXTO_LONGO,
    ESCOLHA_UNICA,
    MULTIPLA_ESCOLHA,
    DATA,
    NUMERO;

    public boolean temOpcoes() {
        return this == ESCOLHA_UNICA || this == MULTIPLA_ESCOLHA;
    }

    public boolean simNao() {
        return this == SIM_NAO || this == SIM_NAO_DETALHE;
    }
}
