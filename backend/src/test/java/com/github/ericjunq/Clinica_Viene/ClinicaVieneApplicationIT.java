package com.github.ericjunq.Clinica_Viene;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Sobe o contexto completo contra um PostgreSQL real (Testcontainers), com Flyway + validação do schema. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ClinicaVieneApplicationIT {

    @Test
    void contextLoads() {}
}
