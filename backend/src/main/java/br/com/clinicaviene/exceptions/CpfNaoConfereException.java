package br.com.clinicaviene.exceptions;

/** CPF informado para baixar um documento não confere (a tentativa é contada). */
public class CpfNaoConfereException extends RegraNegocioException {

    public CpfNaoConfereException() {
        super("O CPF informado não confere com o cadastro.");
    }
}
