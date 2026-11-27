package br.com.clinicaviene.exceptions;

/** Falha ao falar com a ZapSign (fora do ar, token inválido, documento recusado). Vira HTTP 502. */
public class ZapSignException extends RuntimeException {

    public ZapSignException(String mensagem) {
        super(mensagem);
    }

    public ZapSignException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
