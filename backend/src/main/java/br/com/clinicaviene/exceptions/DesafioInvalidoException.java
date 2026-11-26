package br.com.clinicaviene.exceptions;

/** Desafio do login expirado, já usado, esgotado ou inexistente: é preciso informar a senha de novo. */
public class DesafioInvalidoException extends RuntimeException {

    public DesafioInvalidoException() {
        super("Login expirado ou inválido. Entre novamente.");
    }
}
