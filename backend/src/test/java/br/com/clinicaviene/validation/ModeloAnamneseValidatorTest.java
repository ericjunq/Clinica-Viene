package br.com.clinicaviene.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModeloAnamneseValidatorTest {

    @Test
    void aceitaModeloConsistente() {
        assertThatCode(() -> ModeloAnamneseValidator.validar(List.of(
                        pergunta("alergia", TipoPergunta.SIM_NAO_DETALHE, null, true),
                        pergunta("fumo", TipoPergunta.ESCOLHA_UNICA, List.of("Nunca", "Sim"), false))))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaModeloVazioCodigoRepetidoOuInvalido() {
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of())).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of(
                        pergunta("a1", TipoPergunta.SIM_NAO, null, false), pergunta("a1", TipoPergunta.SIM_NAO, null, false))))
                .hasMessageContaining("código repetido");
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of(pergunta("Com Espaço", TipoPergunta.SIM_NAO, null, false))))
                .hasMessageContaining("código");
    }

    @Test
    void opcoesSoEmEscolhaEComPeloMenosDuas() {
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of(pergunta("e1", TipoPergunta.ESCOLHA_UNICA, List.of("Só uma"), false))))
                .hasMessageContaining("2 opções");
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of(pergunta("e2", TipoPergunta.TEXTO_CURTO, List.of("a", "b"), false))))
                .hasMessageContaining("só valem para perguntas de escolha");
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of(pergunta("e3", TipoPergunta.MULTIPLA_ESCOLHA, List.of("a", "a"), false))))
                .hasMessageContaining("repetidas");
    }

    @Test
    void detalheObrigatorioSoEmSimNaoComDetalhe() {
        assertThatThrownBy(() -> ModeloAnamneseValidator.validar(List.of(pergunta("d1", TipoPergunta.SIM_NAO, null, true))))
                .hasMessageContaining("detalhe obrigatório");
    }

    private static PerguntaAnamneseDto pergunta(String codigo, TipoPergunta tipo, List<String> opcoes, boolean detalhe) {
        return new PerguntaAnamneseDto(codigo, "Seção", "Pergunta " + codigo + "?", tipo, true, opcoes, false, detalhe, null);
    }
}
