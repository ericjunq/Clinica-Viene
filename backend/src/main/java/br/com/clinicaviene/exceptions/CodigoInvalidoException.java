package br.com.clinicaviene.exceptions;

/** Código do 2FA (app ou recuperação) errado ou já usado. */
public class CodigoInvalidoException extends RuntimeException {

    public CodigoInvalidoException() {
        super("Código inválido.");
    }
}
