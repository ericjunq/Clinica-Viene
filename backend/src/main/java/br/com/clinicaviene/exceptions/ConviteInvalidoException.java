package br.com.clinicaviene.exceptions;

/** Link de cadastro inexistente, expirado, cancelado ou já usado. Mensagem única (não revela qual). */
public class ConviteInvalidoException extends RecursoNaoEncontradoException {

    public ConviteInvalidoException() {
        super("Este link de cadastro é inválido ou expirou. Peça um novo à recepção da clínica.");
    }
}
