package br.com.clinicaviene.enums;

/** Termos versionados (D-093). Só a política de privacidade é obrigatória (ciência); os demais são opcionais. */
public enum TipoTermo {
    /** Ciência da política de privacidade — obrigatória; não se revoga (base legal de saúde/obrigação legal). */
    POLITICA_PRIVACIDADE(true),
    /** Lembretes de consulta por WhatsApp/SMS. */
    CONSENTIMENTO_LEMBRETES(false),
    /** Promoções e novidades. */
    CONSENTIMENTO_PROMOCOES(false),
    /** Uso de fotos de antes/depois em divulgação. */
    CONSENTIMENTO_IMAGEM(false),
    /** Pesquisas de satisfação. */
    CONSENTIMENTO_PESQUISA(false);

    private final boolean obrigatorio;

    TipoTermo(boolean obrigatorio) {
        this.obrigatorio = obrigatorio;
    }

    public boolean obrigatorio() {
        return obrigatorio;
    }
}
