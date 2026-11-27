package br.com.clinicaviene.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Signatário no formato da API da ZapSign. A clínica entrega o link de assinatura (nada de e-mail/WhatsApp
 * automático da ZapSign) e o nome vem travado do cadastro.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZapSignSignatarioRequest(
        String name,
        String email,
        @JsonProperty("auth_mode") String authMode,
        @JsonProperty("send_automatic_email") boolean sendAutomaticEmail,
        @JsonProperty("send_automatic_whatsapp") boolean sendAutomaticWhatsapp,
        @JsonProperty("lock_name") boolean lockName,
        @JsonProperty("external_id") String externalId) {

    /** Assinatura na tela (desenhada ou digitada), sem envio automático; o link é mostrado pelo sistema. */
    public static ZapSignSignatarioRequest assinaturaNaTela(String nome, String idExterno) {
        return new ZapSignSignatarioRequest(nome, null, "assinaturaTela", false, false, true, idExterno);
    }
}
