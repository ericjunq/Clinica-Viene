package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Storage privado de documentos, S3 compatível (D-056/D-060): SeaweedFS em dev/teste; produção A CONFIRMAR.
 *
 * @param endpoint ex.: {@code http://localhost:8333}
 * @param criarBucket cria o bucket se não existir (dev/teste); em produção o bucket já existe
 */
@Validated
@ConfigurationProperties("clinicaviene.armazenamento")
public record ArmazenamentoProperties(
        @NotBlank String endpoint,
        @NotBlank String regiao,
        @NotBlank String bucket,
        @NotBlank String chaveAcesso,
        @NotBlank String chaveSecreta,
        boolean criarBucket) {

    @Override
    public String toString() {
        return "ArmazenamentoProperties[endpoint=" + endpoint + ", bucket=" + bucket + "]";
    }
}
