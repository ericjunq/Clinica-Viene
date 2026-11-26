package br.com.clinicaviene.compartilhado.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentação OpenAPI da API {@code /api/v1} (exposta só com OPENAPI_HABILITADO=true). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "jwt";

    @Bean
    OpenAPI documentacaoDaApi() {
        return new OpenAPI()
                .info(new Info().title("ClinicaViene API").version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                ESQUEMA_JWT,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
