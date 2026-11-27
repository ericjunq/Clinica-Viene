package br.com.clinicaviene;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Mesmas versões do compose.yaml (D-050). Nunca usar "latest". */
    static final String IMAGEM_POSTGRES = "postgres:18.7-alpine";

    static final String IMAGEM_SEAWEEDFS = "chrislusf/seaweedfs:4.55";
    private static final int PORTA_S3 = 8333;

    /** Um storage para todos os contextos de teste (os documentos têm chaves únicas). */
    private static GenericContainer<?> seaweedfs;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse(IMAGEM_POSTGRES).asCompatibleSubstituteFor("postgres"));
    }

    @Bean
    DynamicPropertyRegistrar armazenamentoDeTeste() {
        String endpoint = iniciarSeaweedfs();
        return registro -> {
            registro.add("clinicaviene.armazenamento.endpoint", () -> endpoint);
            registro.add("clinicaviene.armazenamento.bucket", () -> "clinicaviene-teste");
            registro.add("clinicaviene.armazenamento.criar-bucket", () -> "true");
        };
    }

    private static synchronized String iniciarSeaweedfs() {
        if (seaweedfs == null) {
            GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(IMAGEM_SEAWEEDFS))
                    .withCommand("server", "-dir=/data", "-s3", "-s3.port=" + PORTA_S3)
                    .withExposedPorts(PORTA_S3)
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
            container.start();
            esperarGravacao("http://" + container.getHost() + ":" + container.getMappedPort(PORTA_S3));
            seaweedfs = container;
        }
        return "http://" + seaweedfs.getHost() + ":" + seaweedfs.getMappedPort(PORTA_S3);
    }

    /** A porta abre antes de o SeaweedFS aceitar gravações (volumes ainda subindo): tenta por até 1 min. */
    private static void esperarGravacao(String endpoint) {
        Instant limite = Instant.now().plus(Duration.ofMinutes(1));
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("teste", "teste")))
                .forcePathStyle(true)
                .build()) {
            while (true) {
                try {
                    try {
                        s3.createBucket(r -> r.bucket("aquecimento"));
                    } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException jaExiste) {
                        // tentativa anterior já criou
                    }
                    s3.putObject(r -> r.bucket("aquecimento").key("ok"), RequestBody.fromString("ok"));
                    return;
                } catch (RuntimeException e) {
                    if (Instant.now().isAfter(limite)) {
                        throw new IllegalStateException("SeaweedFS não ficou pronto para gravação", e);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException interrompido) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrompido);
                    }
                }
            }
        }
    }
}
