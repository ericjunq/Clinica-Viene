package br.com.clinicaviene.compartilhado.cripto;

/** Erro ao cifrar/decifrar. A mensagem nunca contém o valor em claro. */
public class FalhaCriptografiaException extends RuntimeException {

    public FalhaCriptografiaException(String mensagem) {
        super(mensagem);
    }

    public FalhaCriptografiaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
