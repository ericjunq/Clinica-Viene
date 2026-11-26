package br.com.clinicaviene.exceptions;

/** Token de renovação inválido, expirado, revogado ou reusado: o usuário precisa fazer login de novo. */
public class SessaoInvalidaException extends RuntimeException {

    public SessaoInvalidaException() {
        super("Sessão inválida ou expirada.");
    }
}
