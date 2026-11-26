package br.com.clinicaviene.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Regras de envio e reenvio de códigos por e-mail (D-073).
 *
 * @param esperas espera mínima antes do 2º, 3º, 4º, 5º envio (o último valor vale para os seguintes)
 * @param maximoEnvios envios permitidos dentro de {@code bloqueio}; depois, só após {@code bloqueio}
 * @param bloqueio janela do limite de envios
 * @param validade quanto tempo o código vale
 * @param tentativas códigos errados até o código ser invalidado
 */
@Validated
@ConfigurationProperties("clinicaviene.envio-codigo")
public record EnvioCodigoProperties(
        @NotEmpty List<Duration> esperas,
        @Min(1) int maximoEnvios,
        @NotNull Duration bloqueio,
        @NotNull Duration validade,
        @Min(1) int tentativas) {}
