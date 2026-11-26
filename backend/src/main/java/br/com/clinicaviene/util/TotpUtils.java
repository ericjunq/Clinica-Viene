package br.com.clinicaviene.util;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP (RFC 6238) compatível com Google Authenticator, Microsoft Authenticator etc.: HMAC-SHA1, passos de
 * 30 s, 6 dígitos (D-072). Implementado com {@code javax.crypto}, sem biblioteca externa.
 */
public final class TotpUtils {

    public static final int SEGUNDOS_POR_PASSO = 30;
    public static final int DIGITOS = 6;
    private static final int BYTES_SEGREDO = 20;
    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TotpUtils() {}

    /** Segredo novo de 160 bits, em Base32 (formato que os apps autenticadores usam). */
    public static String gerarSegredo() {
        byte[] bytes = new byte[BYTES_SEGREDO];
        ALEATORIO.nextBytes(bytes);
        return base32(bytes);
    }

    public static long passo(Instant instante) {
        return Math.floorDiv(instante.getEpochSecond(), SEGUNDOS_POR_PASSO);
    }

    /** Código do passo informado, com a quantidade de dígitos pedida. */
    public static String codigo(byte[] segredo, long passo, int digitos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(segredo, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(passo).array());
            int deslocamento = hash[hash.length - 1] & 0x0f;
            int binario = ((hash[deslocamento] & 0x7f) << 24)
                    | ((hash[deslocamento + 1] & 0xff) << 16)
                    | ((hash[deslocamento + 2] & 0xff) << 8)
                    | (hash[deslocamento + 3] & 0xff);
            int modulo = (int) Math.pow(10, digitos);
            return String.format("%0" + digitos + "d", binario % modulo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String codigo(String segredoBase32, long passo) {
        return codigo(deBase32(segredoBase32), passo, DIGITOS);
    }

    /**
     * Confere o código aceitando {@code janela} passos antes e depois (relógio do celular adiantado ou
     * atrasado). Devolve o passo que bateu, para o chamador impedir reuso.
     */
    public static OptionalLong verificar(String segredoBase32, String codigo, long passoAtual, int janela) {
        if (codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
            return OptionalLong.empty();
        }
        byte[] segredo = deBase32(segredoBase32);
        for (long passo = passoAtual - janela; passo <= passoAtual + janela; passo++) {
            if (igual(codigo(segredo, passo, DIGITOS), codigo)) {
                return OptionalLong.of(passo);
            }
        }
        return OptionalLong.empty();
    }

    /** URI {@code otpauth://} que vira o QR code lido pelo app autenticador. */
    public static String uriOtpAuth(String emissor, String conta, String segredoBase32) {
        String rotulo = codificar(emissor) + ":" + codificar(conta);
        return "otpauth://totp/" + rotulo + "?secret=" + segredoBase32 + "&issuer=" + codificar(emissor)
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + SEGUNDOS_POR_PASSO;
    }

    public static String base32(byte[] dados) {
        StringBuilder saida = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : dados) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                saida.append(ALFABETO_BASE32.charAt((buffer >> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            saida.append(ALFABETO_BASE32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return saida.toString();
    }

    public static byte[] deBase32(String texto) {
        String limpo = texto.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer saida = ByteBuffer.allocate(limpo.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : limpo.toCharArray()) {
            int valor = ALFABETO_BASE32.indexOf(c);
            if (valor < 0) {
                throw new IllegalArgumentException("Base32 inválido");
            }
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                saida.put((byte) ((buffer >> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        return saida.array();
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Comparação em tempo constante (não vaza, pelo tempo de resposta, quantos dígitos acertaram). */
    private static boolean igual(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }
}
