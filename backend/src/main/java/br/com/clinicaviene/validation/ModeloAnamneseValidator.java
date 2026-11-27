package br.com.clinicaviene.validation;

import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Consistência das perguntas de um modelo de anamnese (D-089). */
public final class ModeloAnamneseValidator {

    private static final String PADRAO_CODIGO = "[a-z][a-z0-9_]{1,59}";

    private ModeloAnamneseValidator() {}

    public static void validar(List<PerguntaAnamneseDto> perguntas) {
        if (perguntas == null || perguntas.isEmpty()) {
            throw new RegraNegocioException("O modelo precisa de pelo menos uma pergunta.");
        }
        List<String> erros = new ArrayList<>();
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < perguntas.size(); i++) {
            PerguntaAnamneseDto p = perguntas.get(i);
            String onde = "Pergunta " + (i + 1);
            if (p == null || p.tipo() == null || p.texto() == null || p.texto().isBlank()) {
                erros.add(onde + ": informe o texto e o tipo");
                continue;
            }
            if (p.codigo() == null || !p.codigo().matches(PADRAO_CODIGO)) {
                erros.add(onde + ": código deve ter letras minúsculas, números e _ (ex.: alergia_medicamento)");
            } else if (!codigos.add(p.codigo())) {
                erros.add(onde + ": código repetido (" + p.codigo() + ")");
            }
            List<String> opcoes = p.opcoes() == null ? List.of() : p.opcoes();
            if (p.tipo().temOpcoes()) {
                if (opcoes.size() < 2) {
                    erros.add(onde + ": escolha precisa de pelo menos 2 opções");
                } else if (new HashSet<>(opcoes).size() != opcoes.size()) {
                    erros.add(onde + ": opções repetidas");
                }
            } else if (!opcoes.isEmpty()) {
                erros.add(onde + ": opções só valem para perguntas de escolha");
            }
            if (p.exigeDetalheQuandoSim() && p.tipo() != TipoPergunta.SIM_NAO_DETALHE) {
                erros.add(onde + ": detalhe obrigatório só vale para Sim/Não com detalhe");
            }
        }
        if (!erros.isEmpty()) {
            throw new RegraNegocioException("Modelo inválido: " + String.join("; ", erros) + ".");
        }
    }
}
