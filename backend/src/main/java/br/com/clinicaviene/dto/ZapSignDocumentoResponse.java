package br.com.clinicaviene.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Documento devolvido pela ZapSign. {@code originalFile}/{@code signedFile} são links temporários (60 min): baixar e
 * guardar no storage próprio, nunca persistir o link.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ZapSignDocumentoResponse(
        String token,
        String status,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("original_file") String originalFile,
        @JsonProperty("signed_file") String signedFile,
        List<Signatario> signers) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Signatario(
            String token,
            String status,
            String name,
            @JsonProperty("sign_url") String signUrl,
            @JsonProperty("external_id") String externalId,
            @JsonProperty("signed_at") String signedAt) {}
}
