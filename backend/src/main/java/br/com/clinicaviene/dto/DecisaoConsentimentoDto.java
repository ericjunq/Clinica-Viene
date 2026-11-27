package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.TipoTermo;

/** O que o paciente marcou (ou deixou desmarcado) para um termo. */
public record DecisaoConsentimentoDto(TipoTermo tipo, boolean aceito) {}
