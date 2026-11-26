package br.com.clinicaviene.exceptions;


/** Usuário inexistente, ou que quem consulta não pode saber que existe (ex.: MASTER consultando DEV). */
public class UsuarioNaoEncontradoException extends RecursoNaoEncontradoException {

    public UsuarioNaoEncontradoException() {
        super("Usuário não encontrado.");
    }
}
