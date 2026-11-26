package br.com.clinicaviene.util;

import br.com.clinicaviene.exceptions.CriptografiaException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Criptografia de campos sensíveis com AES-256-GCM e chave versionada (D-052).
 *
 * <p>Formato do valor cifrado: {@code <versão>:<base64(iv || texto cifrado + tag)>}. A versão da chave vai
 * junto do valor (e entra como dado autenticado no GCM), o que permite rotação: novos valores usam a chave
 * ativa e valores antigos continuam legíveis enquanto a versão antiga estiver configurada.
 */
public class CifradorAesGcm {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_CHAVE_BYTES = 32;
    private static final int TAMANHO_IV_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;
    private static final String SEPARADOR = ":";

    private final Map<String, SecretKey> chaves = new HashMap<>();
    private final String versaoAtiva;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorAesGcm(Map<String, byte[]> chavesPorVersao, String versaoAtiva) {
        if (chavesPorVersao == null || chavesPorVersao.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma chave de criptografia configurada");
        }
        chavesPorVersao.forEach((versao, chave) -> {
            if (versao == null || versao.isBlank() || versao.contains(SEPARADOR)) {
                throw new IllegalArgumentException("Versão de chave inválida: '" + versao + "'");
            }
            if (chave == null || chave.length != TAMANHO_CHAVE_BYTES) {
                throw new IllegalArgumentException(
                        "A chave da versão '" + versao + "' precisa ter " + TAMANHO_CHAVE_BYTES + " bytes (AES-256)");
            }
            chaves.put(versao, new SecretKeySpec(chave, "AES"));
        });
        if (!chaves.containsKey(versaoAtiva)) {
            throw new IllegalArgumentException("A versão ativa '" + versaoAtiva + "' não está entre as chaves configuradas");
        }
        this.versaoAtiva = versaoAtiva;
    }

    public String versaoAtiva() {
        return versaoAtiva;
    }

    /** Cifra com a chave ativa. {@code null} continua {@code null}. */
    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        byte[] iv = new byte[TAMANHO_IV_BYTES];
        aleatorio.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, chaves.get(versaoAtiva), new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            cipher.updateAAD(versaoAtiva.getBytes(StandardCharsets.UTF_8));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return versaoAtiva + SEPARADOR + Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new CriptografiaException("Falha ao cifrar valor", e);
        }
    }

    /** Decifra um valor produzido por {@link #cifrar}. {@code null} continua {@code null}. */
    public String decifrar(String valor) {
        if (valor == null) {
            return null;
        }
        int posicao = valor.indexOf(SEPARADOR);
        if (posicao <= 0) {
            throw new CriptografiaException("Valor cifrado em formato inválido");
        }
        String versao = valor.substring(0, posicao);
        SecretKey chave = chaves.get(versao);
        if (chave == null) {
            throw new CriptografiaException("Versão de chave desconhecida: '" + versao + "'");
        }
        try {
            byte[] dados = Base64.getDecoder().decode(valor.substring(posicao + 1));
            if (dados.length <= TAMANHO_IV_BYTES) {
                throw new CriptografiaException("Valor cifrado em formato inválido");
            }
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    chave,
                    new GCMParameterSpec(TAMANHO_TAG_BITS, dados, 0, TAMANHO_IV_BYTES));
            cipher.updateAAD(versao.getBytes(StandardCharsets.UTF_8));
            byte[] texto = cipher.doFinal(dados, TAMANHO_IV_BYTES, dados.length - TAMANHO_IV_BYTES);
            return new String(texto, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new CriptografiaException("Valor cifrado em formato inválido", e);
        } catch (GeneralSecurityException e) {
            throw new CriptografiaException("Valor cifrado inválido ou adulterado", e);
        }
    }
}
