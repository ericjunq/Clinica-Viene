package br.com.clinicaviene.exceptions;

import lombok.Getter;

/** Limite de tentativas atingido (rate limit). Vira HTTP 429 com o cabeçalho {@code Retry-After}. */
@Getter
public class MuitasTentativasException extends RuntimeException {

    private final long segundosParaTentarDeNovo;

    public MuitasTentativasException(long segundosParaTentarDeNovo) {
        super("Muitas tentativas. Tente novamente mais tarde.");
        this.segundosParaTentarDeNovo = Math.max(1, segundosParaTentarDeNovo);
    }
}
