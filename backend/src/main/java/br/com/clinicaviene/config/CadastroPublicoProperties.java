package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Preenchimento do cadastro pelo paciente (D-100).
 *
 * @param validadeSessao quanto tempo o paciente tem para preencher depois de confirmar a identidade
 */
@Validated
@ConfigurationProperties("clinicaviene.cadastro-publico")
public record CadastroPublicoProperties(@NotNull Duration validadeSessao) {}
