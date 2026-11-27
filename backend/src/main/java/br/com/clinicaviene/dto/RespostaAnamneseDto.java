package br.com.clinicaviene.dto;

import java.util.List;

/**
 * Resposta a uma pergunta da anamnese.
 *
 * @param valor {@code SIM}/{@code NAO}, texto, opção escolhida, data ISO ({@code aaaa-MM-dd}) ou número
 * @param valores opções marcadas ({@code MULTIPLA_ESCOLHA})
 * @param detalhe o "qual?" de {@code SIM_NAO_DETALHE}
 */
public record RespostaAnamneseDto(String valor, List<String> valores, String detalhe) {

    @Override
    public String toString() {
        return "RespostaAnamneseDto[***]";
    }
}
