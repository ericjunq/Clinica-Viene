package br.com.clinicaviene;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    /** Mesma versão do compose.yaml (D-050). Nunca usar "latest". */
    static final String IMAGEM_POSTGRES = "postgres:18.7-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse(IMAGEM_POSTGRES).asCompatibleSubstituteFor("postgres"));
    }
}
