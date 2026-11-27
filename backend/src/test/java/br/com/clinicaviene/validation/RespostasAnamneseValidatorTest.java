package br.com.clinicaviene.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.RespostaAnamneseDto;
import br.com.clinicaviene.enums.TipoPergunta;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RespostasAnamneseValidatorTest {

    private static final List<PerguntaAnamneseDto> PERGUNTAS = List.of(
            new PerguntaAnamneseDto("alergia", "S", "Tem alergia?", TipoPergunta.SIM_NAO_DETALHE, true, null, true, true, null),
            new PerguntaAnamneseDto("pressao", "S", "Pressão alta?", TipoPergunta.SIM_NAO, true, null, true, false, null),
            new PerguntaAnamneseDto("gravidez", "S", "Grávida?", TipoPergunta.ESCOLHA_UNICA, false, List.of("Não", "Sim", "Não sei"), true, false, null),
            new PerguntaAnamneseDto("habitos", "S", "Hábitos?", TipoPergunta.MULTIPLA_ESCOLHA, false, List.of("Nenhum", "Roer unhas", "Bruxismo"), false, false, null),
            new PerguntaAnamneseDto("ultima_consulta", "S", "Data da última consulta", TipoPergunta.DATA, false, null, false, false, null),
            new PerguntaAnamneseDto("dor", "S", "Dor de 0 a 10", TipoPergunta.NUMERO, false, null, false, false, null),
            new PerguntaAnamneseDto("obs", "S", "Observações", TipoPergunta.TEXTO_LONGO, false, null, false, false, null));

    @Test
    void respostasValidasDevolvemOsAlertasPositivos() {
        Map<String, RespostaAnamneseDto> respostas = new HashMap<>();
        respostas.put("alergia", new RespostaAnamneseDto("SIM", null, "Penicilina"));
        respostas.put("pressao", new RespostaAnamneseDto("NAO", null, null));
        respostas.put("gravidez", new RespostaAnamneseDto("Não sei", null, null));
        respostas.put("habitos", new RespostaAnamneseDto(null, List.of("Roer unhas", "Bruxismo"), null));
        respostas.put("ultima_consulta", new RespostaAnamneseDto("2025-03-10", null, null));
        respostas.put("dor", new RespostaAnamneseDto("7,5", null, null));

        assertThat(RespostasAnamneseValidator.validar(PERGUNTAS, respostas)).containsExactly("alergia", "gravidez");
    }

    @Test
    void primeiraOpcaoDaEscolhaENormalSemAlerta() {
        Map<String, RespostaAnamneseDto> respostas = basicas();
        respostas.put("gravidez", new RespostaAnamneseDto("Não", null, null));

        assertThat(RespostasAnamneseValidator.validar(PERGUNTAS, respostas)).isEmpty();
    }

    @Test
    void exigeObrigatoriasEDetalheQuandoSim() {
        assertThatThrownBy(() -> RespostasAnamneseValidator.validar(PERGUNTAS, Map.of()))
                .hasMessageContaining("Tem alergia?")
                .hasMessageContaining("Pressão alta?");
        Map<String, RespostaAnamneseDto> semDetalhe = basicas();
        semDetalhe.put("alergia", new RespostaAnamneseDto("SIM", null, " "));
        assertThatThrownBy(() -> RespostasAnamneseValidator.validar(PERGUNTAS, semDetalhe)).hasMessageContaining("informe qual");
    }

    @Test
    void recusaFormatosInvalidosEPerguntasDesconhecidas() {
        Map<String, RespostaAnamneseDto> invalidas = basicas();
        invalidas.put("pressao", new RespostaAnamneseDto("talvez", null, null));
        invalidas.put("gravidez", new RespostaAnamneseDto("Outra", null, null));
        invalidas.put("habitos", new RespostaAnamneseDto(null, List.of("Inventado"), null));
        invalidas.put("ultima_consulta", new RespostaAnamneseDto("31/02/2025", null, null));
        invalidas.put("dor", new RespostaAnamneseDto("muita", null, null));
        invalidas.put("obs", new RespostaAnamneseDto(null, List.of("x"), null));
        invalidas.put("nao_existe", new RespostaAnamneseDto("SIM", null, null));

        assertThatThrownBy(() -> RespostasAnamneseValidator.validar(PERGUNTAS, invalidas))
                .hasMessageContaining("responda Sim ou Não")
                .hasMessageContaining("opção inválida")
                .hasMessageContaining("opções inválidas")
                .hasMessageContaining("data inválida")
                .hasMessageContaining("número inválido")
                .hasMessageContaining("resposta inválida")
                .hasMessageContaining("pergunta desconhecida: nao_existe");
    }

    private static Map<String, RespostaAnamneseDto> basicas() {
        Map<String, RespostaAnamneseDto> r = new HashMap<>();
        r.put("alergia", new RespostaAnamneseDto("NAO", null, null));
        r.put("pressao", new RespostaAnamneseDto("NAO", null, null));
        return r;
    }
}
