package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Convites de cadastro (D-091).
 *
 * @param urlPublica endereço do frontend usado no link (ex.: {@code https://sistema.clinicaviene.com.br})
 * @param validadeLink convite para o celular do paciente (padrão 72 h)
 * @param validadeQuiosque convite aberto num aparelho da clínica (padrão 2 h)
 */
@Validated
@ConfigurationProperties("clinicaviene.convite")
public record ConviteProperties(
        @NotBlank String urlPublica, @NotNull Duration validadeLink, @NotNull Duration validadeQuiosque) {}
