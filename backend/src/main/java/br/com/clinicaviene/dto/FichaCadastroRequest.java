package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ficha completa preenchida pelo paciente: dados conferidos, anamnese e decisões sobre os termos. Revisada na 2E
 * e assinada (gravada) na 2F.
 *
 * @param modeloAnamneseId versão do modelo que o paciente respondeu (precisa ser a vigente)
 * @param respostas por código da pergunta
 * @param termos uma decisão para cada termo vigente mostrado
 * @param acompanhante opcional
 */
public record FichaCadastroRequest(
        @NotNull @Valid DadosCadastroDto dados,
        @NotNull UUID modeloAnamneseId,
        Map<String, RespostaAnamneseDto> respostas,
        @NotNull List<@NotNull @Valid DecisaoTermoRequest> termos,
        @Valid AcompanhanteDto acompanhante) {

    @Override
    public String toString() {
        return "FichaCadastroRequest[modeloAnamneseId=" + modeloAnamneseId + "]";
    }
}
