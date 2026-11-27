package br.com.clinicaviene.config;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Cliente S3 com endpoint configurável (qualquer storage S3 compatível — D-056). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ArmazenamentoProperties.class)
public class ArmazenamentoConfig {

    @Bean(destroyMethod = "close")
    S3Client s3Client(ArmazenamentoProperties propriedades) {
        return S3Client.builder()
                .endpointOverride(URI.create(propriedades.endpoint()))
                .region(Region.of(propriedades.regiao()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(propriedades.chaveAcesso(), propriedades.chaveSecreta())))
                // endereços no formato endpoint/bucket/chave (SeaweedFS, MinIO e afins)
                .forcePathStyle(true)
                .build();
    }
}
