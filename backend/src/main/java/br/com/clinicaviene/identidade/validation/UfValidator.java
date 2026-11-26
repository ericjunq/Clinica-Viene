package br.com.clinicaviene.identidade.validation;

import java.util.Set;

/** Unidades da federação, para o registro profissional. */
public final class UfValidator {

    private static final Set<String> SIGLAS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE",
            "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private UfValidator() {}

    public static boolean valida(String sigla) {
        return sigla != null && SIGLAS.contains(sigla);
    }
}
