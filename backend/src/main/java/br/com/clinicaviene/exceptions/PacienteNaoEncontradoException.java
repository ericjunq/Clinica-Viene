package br.com.clinicaviene.exceptions;

public class PacienteNaoEncontradoException extends RecursoNaoEncontradoException {

    public PacienteNaoEncontradoException() {
        super("Paciente não encontrado.");
    }
}
