package br.com.clinicaviene.validation;

import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.RespostaAnamneseDto;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Confere as respostas do paciente contra a versão do modelo usada (D-089): obrigatórias, formato por tipo,
 * opções válidas. Devolve os códigos das perguntas de alerta clínico com resposta positiva.
 */
public final class RespostasAnamneseValidator {

    public static final String SIM = "SIM";
    public static final String NAO = "NAO";
    private static final int TAMANHO_TEXTO_CURTO = 300;
    private static final int TAMANHO_TEXTO_LONGO = 4000;

    private RespostasAnamneseValidator() {}

    /** @return códigos das perguntas de alerta clínico respondidas positivamente */
    public static List<String> validar(List<PerguntaAnamneseDto> perguntas, Map<String, RespostaAnamneseDto> respostas) {
        Map<String, RespostaAnamneseDto> r = respostas == null ? Map.of() : respostas;
        Set<String> conhecidas = new HashSet<>();
        List<String> erros = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        for (PerguntaAnamneseDto p : perguntas) {
            conhecidas.add(p.codigo());
            RespostaAnamneseDto resposta = r.get(p.codigo());
            if (vazia(resposta)) {
                if (p.ehObrigatoria()) {
                    erros.add("responda: " + p.texto());
                }
                continue;
            }
            String erro = validarUma(p, resposta);
            if (erro != null) {
                erros.add(p.texto() + " — " + erro);
            } else if (p.ehAlertaClinico() && positiva(p, resposta)) {
                alertas.add(p.codigo());
            }
        }
        for (String codigo : r.keySet()) {
            if (!conhecidas.contains(codigo)) {
                erros.add("pergunta desconhecida: " + codigo);
            }
        }
        if (!erros.isEmpty()) {
            throw new RegraNegocioException("Anamnese incompleta ou inválida: " + String.join("; ", erros) + ".");
        }
        return alertas;
    }

    private static String validarUma(PerguntaAnamneseDto p, RespostaAnamneseDto resposta) {
        String valor = resposta.valor() == null ? null : resposta.valor().trim();
        List<String> opcoes = p.opcoes() == null ? List.of() : p.opcoes();
        if (p.tipo() != TipoPergunta.MULTIPLA_ESCOLHA && vazio(valor)) {
            return "resposta inválida";
        }
        switch (p.tipo()) {
            case SIM_NAO, SIM_NAO_DETALHE -> {
                if (!SIM.equals(valor) && !NAO.equals(valor)) {
                    return "responda Sim ou Não";
                }
                if (SIM.equals(valor) && p.exigeDetalheQuandoSim() && vazio(resposta.detalhe())) {
                    return "informe qual";
                }
                if (resposta.detalhe() != null && resposta.detalhe().length() > TAMANHO_TEXTO_CURTO) {
                    return "detalhe muito longo";
                }
            }
            case TEXTO_CURTO -> {
                if (valor.length() > TAMANHO_TEXTO_CURTO) {
                    return "texto muito longo (máximo " + TAMANHO_TEXTO_CURTO + ")";
                }
            }
            case TEXTO_LONGO -> {
                if (valor.length() > TAMANHO_TEXTO_LONGO) {
                    return "texto muito longo (máximo " + TAMANHO_TEXTO_LONGO + ")";
                }
            }
            case ESCOLHA_UNICA -> {
                if (!opcoes.contains(valor)) {
                    return "opção inválida";
                }
            }
            case MULTIPLA_ESCOLHA -> {
                List<String> valores = resposta.valores() == null ? List.of() : resposta.valores();
                if (valores.isEmpty() || !opcoes.containsAll(valores) || new HashSet<>(valores).size() != valores.size()) {
                    return "opções inválidas";
                }
            }
            case DATA -> {
                try {
                    LocalDate.parse(valor);
                } catch (DateTimeParseException e) {
                    return "data inválida";
                }
            }
            case NUMERO -> {
                try {
                    new BigDecimal(valor.replace(',', '.'));
                } catch (NumberFormatException e) {
                    return "número inválido";
                }
            }
        }
        return null;
    }

    /**
     * "Positiva" para alerta: Sim nas perguntas Sim/Não; nas de escolha, qualquer opção diferente da primeira
     * (convenção: a primeira opção é a resposta "normal"); nas demais, qualquer resposta preenchida.
     */
    private static boolean positiva(PerguntaAnamneseDto p, RespostaAnamneseDto resposta) {
        if (p.tipo().simNao()) {
            return SIM.equals(resposta.valor().trim());
        }
        if (p.tipo().temOpcoes()) {
            String normal = p.opcoes().get(0);
            List<String> marcadas = p.tipo() == TipoPergunta.MULTIPLA_ESCOLHA
                    ? resposta.valores()
                    : List.of(resposta.valor().trim());
            return marcadas.stream().anyMatch(v -> !v.equals(normal));
        }
        return !vazia(resposta);
    }

    private static boolean vazia(RespostaAnamneseDto r) {
        return r == null || (vazio(r.valor()) && (r.valores() == null || r.valores().isEmpty()));
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
