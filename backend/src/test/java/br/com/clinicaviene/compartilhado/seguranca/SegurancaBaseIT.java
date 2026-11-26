package br.com.clinicaviene.compartilhado.seguranca;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import br.com.clinicaviene.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Segurança base (1A): tudo fechado por padrão, respostas de erro em Problem Details. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SegurancaBaseIT {

    @Value("${local.server.port}")
    private int porta;

    @Test
    void healthEhPublico() {
        given().port(porta).when().get("/actuator/health").then().statusCode(200);
    }

    @Test
    void apiExigeAutenticacaoERespondeProblemDetails() {
        given().port(porta)
                .when()
                .get("/api/v1/qualquer-coisa")
                .then()
                .statusCode(401)
                .contentType(startsWith("application/problem+json"))
                .body("status", equalTo(401))
                .body("detail", equalTo("Autenticação necessária."));
    }

    @Test
    void openApiDesligadoPorPadrao() {
        // OPENAPI_HABILITADO não está definido nos testes: a documentação não fica exposta.
        given().port(porta).when().get("/v3/api-docs").then().statusCode(401);
    }

    @Test
    void naoCriaSessaoNoServidor() {
        given().port(porta)
                .when()
                .get("/api/v1/qualquer-coisa")
                .then()
                .statusCode(401)
                .header("Set-Cookie", org.hamcrest.Matchers.nullValue());
    }
}
