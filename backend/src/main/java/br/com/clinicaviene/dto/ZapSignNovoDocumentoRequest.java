package br.com.clinicaviene.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Corpo de {@code POST /docs/} da ZapSign (PDF em base64, sem o prefixo {@code data:}). */
public record ZapSignNovoDocumentoRequest(
        String name,
        @JsonProperty("base64_pdf") String base64Pdf,
        String lang,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("disable_signer_emails") boolean disableSignerEmails,
        List<ZapSignSignatarioRequest> signers) {}
