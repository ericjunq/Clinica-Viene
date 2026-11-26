package br.com.clinicaviene.compartilhado.cripto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 do CPF normalizado, com chave secreta (D-052). Usado na coluna {@code cpf_hash} para
 * unicidade e busca sem expor o CPF. Nunca SHA puro: o espaço de CPFs é pequeno e seria quebrado por força
 * bruta.
 */
public class HashCpf {

    private static final String ALGORITMO = "HmacSHA256";
    private static final int TAMANHO_MINIMO_CHAVE_BYTES = 32;

    private final SecretKeySpec chave;

    public HashCpf(byte[] chave) {
        if (chave == null || chave.length < TAMANHO_MINIMO_CHAVE_BYTES) {
            throw new IllegalArgumentException(
                    "A chave do HMAC do CPF precisa ter pelo menos " + TAMANHO_MINIMO_CHAVE_BYTES + " bytes");
        }
        this.chave = new SecretKeySpec(chave, ALGORITMO);
    }

    /** Hash hexadecimal (64 caracteres) do CPF, com ou sem pontuação. */
    public String calcular(String cpf) {
        String digitos = Cpf.normalizar(cpf);
        if (digitos == null || digitos.isEmpty()) {
            throw new IllegalArgumentException("CPF vazio");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(chave);
            return HexFormat.of().formatHex(mac.doFinal(digitos.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new FalhaCriptografiaException("Falha ao calcular o hash do CPF", e);
        }
    }
}
