package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Adaptador ZapSign (D-090, D-103). Desligado até o autor liberar o envio: com {@code habilitado=false} nenhuma
 * chamada sai da aplicação.
 *
 * @param urlApi sandbox {@code https://sandbox.api.zapsign.com.br/api/v1} (sem validade jurídica, não cobra) ou
 *     produção {@code https://api.zapsign.com.br/api/v1}
 * @param token token da API (Configurações → Integrações → API ZapSign); nunca no repositório
 * @param tamanhoMaximoPdf limite da ZapSign: 1 PDF de até 10 MB por documento
 */
@Validated
@ConfigurationProperties("clinicaviene.zapsign")
public record ZapSignProperties(
        boolean habilitado,
        @NotBlank String urlApi,
        String token,
        @NotNull DataSize tamanhoMaximoPdf,
        @NotNull Duration tempoLimite) {

    @Override
    public String toString() {
        return "ZapSignProperties[habilitado=" + habilitado + ", urlApi=" + urlApi + "]";
    }
}
