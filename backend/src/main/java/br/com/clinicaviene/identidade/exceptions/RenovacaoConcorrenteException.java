package br.com.clinicaviene.identidade.exceptions;

/** Token trocado há instantes por outra aba: o cliente deve tentar de novo com o cookie atualizado. */
public class RenovacaoConcorrenteException extends RuntimeException {

    public RenovacaoConcorrenteException() {
        super("Renovação concorrente; tente novamente.");
    }
}
