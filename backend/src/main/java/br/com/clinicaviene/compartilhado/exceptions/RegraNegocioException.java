package br.com.clinicaviene.compartilhado.exceptions;

/** Violação de regra de negócio. Vira HTTP 422 com a mensagem no {@code detail}. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
