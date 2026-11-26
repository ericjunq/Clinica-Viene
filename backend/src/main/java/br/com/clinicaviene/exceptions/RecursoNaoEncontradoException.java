package br.com.clinicaviene.exceptions;

/** Recurso inexistente (ou que o usuário não pode saber que existe). Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
