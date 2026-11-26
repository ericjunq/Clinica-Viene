package br.com.clinicaviene.exceptions;

/** Erro ao cifrar/decifrar. A mensagem nunca contém o valor em claro. */
public class CriptografiaException extends RuntimeException {

    public CriptografiaException(String mensagem) {
        super(mensagem);
    }

    public CriptografiaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
