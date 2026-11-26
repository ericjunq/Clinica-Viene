package br.com.clinicaviene.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Tokens opacos (sessão, desafio): valor aleatório para o cliente, só o SHA-256 no banco. */
public final class TokenUtils {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    private TokenUtils() {}

    /** 256 bits aleatórios em Base64 URL-safe, sem padding. */
    public static String gerar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 em hexadecimal. Suficiente para tokens de alta entropia (não serve para senhas nem para CPF —
     * ver Argon2id e {@link CpfHasher}).
     */
    public static String sha256(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
