package br.com.clinicaviene.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Códigos de recuperação do 2FA (D-072): 16 caracteres (cerca de 79 bits) no formato {@code XXXX-XXXX-XXXX-XXXX}, sem
 * caracteres ambíguos (0/O, 1/I/L).
 */
public final class CodigoRecuperacaoUtils {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoRecuperacaoUtils() {}

    public static String gerar() {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < TAMANHO; i++) {
            if (i > 0 && i % 4 == 0) {
                codigo.append('-');
            }
            codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }

    /** Ignora hífens, espaços e maiúsculas/minúsculas (o usuário digita como quiser). */
    public static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
