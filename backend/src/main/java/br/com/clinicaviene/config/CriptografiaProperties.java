package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Chaves de criptografia, vindas do ambiente (nunca do repositório — D-052). Sem elas a aplicação não sobe.
 *
 * @param chaves versões e chaves AES-256 em Base64, no formato {@code v1:BASE64,v2:BASE64}
 * @param chaveAtiva versão usada para cifrar novos valores
 * @param cpfHmacChave chave do HMAC do CPF, em Base64 (32 bytes ou mais)
 */
@Validated
@ConfigurationProperties("clinicaviene.cripto")
public record CriptografiaProperties(
        @NotBlank(message = "defina CRIPTO_CHAVES") String chaves,
        @NotBlank(message = "defina CRIPTO_CHAVE_ATIVA") String chaveAtiva,
        @NotBlank(message = "defina CPF_HMAC_CHAVE") String cpfHmacChave) {}
