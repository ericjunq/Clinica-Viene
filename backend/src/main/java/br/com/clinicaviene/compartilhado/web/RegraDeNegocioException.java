package br.com.clinicaviene.compartilhado.web;

/** Violação de regra de negócio. Vira HTTP 422 com a mensagem no {@code detail}. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
