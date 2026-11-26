package br.com.clinicaviene;

import java.util.concurrent.ThreadLocalRandom;

/** Gera CPFs fictícios com dígitos verificadores válidos, para testes (nunca usar CPF real). */
public final class CpfsFicticios {

    private CpfsFicticios() {}

    public static String gerar() {
        StringBuilder cpf = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            cpf.append(ThreadLocalRandom.current().nextInt(10));
        }
        cpf.append(digito(cpf, 10));
        cpf.append(digito(cpf, 11));
        String resultado = cpf.toString();
        // Evita a sequência de dígitos iguais (inválida).
        return resultado.chars().distinct().count() == 1 ? gerar() : resultado;
    }

    private static int digito(CharSequence base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
