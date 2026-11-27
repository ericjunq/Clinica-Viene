package br.com.clinicaviene.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Entrega de documentos ao paciente por link + CPF (D-094).
 *
 * @param validade quanto tempo o link vale
 * @param maximoDownloads downloads permitidos por link
 * @param tentativasCpf CPFs errados até o link ser bloqueado
 */
@Validated
@ConfigurationProperties("clinicaviene.entrega-documento")
public record EntregaDocumentoProperties(@NotNull Duration validade, @Min(1) int maximoDownloads, @Min(1) int tentativasCpf) {}
