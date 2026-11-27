package br.com.clinicaviene.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gerador padrão de ids (D-095): UUID versão 7 (RFC 9562) — 48 bits de tempo (ms) + 74 bits aleatórios.
 * Ordenado pelo tempo (inserção eficiente no índice, como uma sequence) e não enumerável em URLs.
 */
public final class IdUtils {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private IdUtils() {}

    public static UUID novoId() {
        return novoId(System.currentTimeMillis());
    }

    static UUID novoId(long epochMillis) {
        long aleatorioA = ALEATORIO.nextInt(1 << 12); // 12 bits
        long aleatorioB = ALEATORIO.nextLong();
        long maisSignificativos = ((epochMillis & 0xFFFF_FFFF_FFFFL) << 16) | (0x7L << 12) | aleatorioA;
        long menosSignificativos = (aleatorioB & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L; // variante 10
        return new UUID(maisSignificativos, menosSignificativos);
    }
}
