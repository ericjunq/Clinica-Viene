package br.com.clinicaviene.compartilhado.util;

/** Normalização e validação de CPF (dígitos verificadores). */
public final class CpfUtils {

    private static final int TAMANHO = 11;

    private CpfUtils() {}

    /** Mantém só os dígitos. {@code null} continua {@code null}. */
    public static String normalizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /** {@code true} se o CPF (com ou sem pontuação) tem 11 dígitos e dígitos verificadores corretos. */
    public static boolean valido(String cpf) {
        String digitos = normalizar(cpf);
        if (digitos == null || digitos.length() != TAMANHO) {
            return false;
        }
        if (digitos.chars().distinct().count() == 1) {
            // 000.000.000-00, 111.111.111-11... passam no cálculo, mas não são CPFs válidos.
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
                && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
