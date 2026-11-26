package br.com.clinicaviene.identidade.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Política de senha (D-066): mínimo 12 caracteres, com pelo menos 1 letra maiúscula, 1 minúscula e 1
 * caractere especial. Máximo de 128 caracteres (limite técnico).
 */
public final class PoliticaSenhaValidator {

    public static final int TAMANHO_MINIMO = 12;
    public static final int TAMANHO_MAXIMO = 128;

    private PoliticaSenhaValidator() {}

    /** Lista de regras não atendidas (vazia = senha válida). */
    public static List<String> violacoes(String senha) {
        List<String> violacoes = new ArrayList<>();
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            violacoes.add("ter pelo menos " + TAMANHO_MINIMO + " caracteres");
        }
        if (senha != null && senha.length() > TAMANHO_MAXIMO) {
            violacoes.add("ter no máximo " + TAMANHO_MAXIMO + " caracteres");
        }
        if (senha == null || senha.chars().noneMatch(Character::isUpperCase)) {
            violacoes.add("ter pelo menos 1 letra maiúscula");
        }
        if (senha == null || senha.chars().noneMatch(Character::isLowerCase)) {
            violacoes.add("ter pelo menos 1 letra minúscula");
        }
        if (senha == null
                || senha.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            violacoes.add("ter pelo menos 1 caractere especial");
        }
        return violacoes;
    }

    public static boolean valida(String senha) {
        return violacoes(senha).isEmpty();
    }
}
