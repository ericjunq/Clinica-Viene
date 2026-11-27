package br.com.clinicaviene.exceptions;

/** Link de download inexistente, expirado, esgotado ou bloqueado por CPFs errados. Mensagem única. */
public class LinkDocumentoInvalidoException extends RecursoNaoEncontradoException {

    public LinkDocumentoInvalidoException() {
        super("Este link para baixar o documento é inválido ou expirou. Peça um novo à clínica.");
    }
}
