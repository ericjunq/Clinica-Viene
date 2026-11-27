package br.com.clinicaviene.exceptions;

/** Sessão de preenchimento do cadastro inexistente, expirada ou de convite já usado/cancelado (401). */
public class SessaoCadastroInvalidaException extends SessaoInvalidaException {

    public SessaoCadastroInvalidaException() {
        super("Sua sessão de preenchimento expirou. Abra o link de novo para continuar.");
    }
}
