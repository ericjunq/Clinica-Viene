package br.com.clinicaviene.exceptions;

/**
 * CPF ou senha errados, usuário inexistente ou inativo. Sempre a mesma mensagem: não revela se o CPF existe
 * nem se o usuário está inativo.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("CPF ou senha inválidos.");
    }
}
