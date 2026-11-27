package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.TipoPergunta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Pergunta de um modelo de anamnese (D-089).
 *
 * @param codigo identificador estável dentro do modelo (ex.: {@code alergia_medicamento})
 * @param secao agrupamento na tela (ex.: "Saúde geral")
 * @param opcoes só para {@code ESCOLHA_UNICA} e {@code MULTIPLA_ESCOLHA}
 * @param alertaClinico resposta positiva vira alerta no topo do prontuário (Fase 4). Em perguntas de escolha, a
 *     <b>primeira opção é a resposta "normal"</b> (sem alerta); as demais acionam o alerta
 * @param detalheObrigatorioQuandoSim em {@code SIM_NAO_DETALHE}, exige o "qual?" quando a resposta é Sim
 */
public record PerguntaAnamneseDto(
        @NotBlank @Size(max = 60) String codigo,
        @Size(max = 80) String secao,
        @NotBlank @Size(max = 300) String texto,
        @NotNull TipoPergunta tipo,
        Boolean obrigatoria,
        List<@NotBlank @Size(max = 120) String> opcoes,
        Boolean alertaClinico,
        Boolean detalheObrigatorioQuandoSim,
        @Size(max = 300) String ajuda) {

    public boolean ehObrigatoria() {
        return Boolean.TRUE.equals(obrigatoria);
    }

    public boolean ehAlertaClinico() {
        return Boolean.TRUE.equals(alertaClinico);
    }

    public boolean exigeDetalheQuandoSim() {
        return Boolean.TRUE.equals(detalheObrigatorioQuandoSim);
    }
}
