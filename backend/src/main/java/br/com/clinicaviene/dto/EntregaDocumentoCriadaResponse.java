package br.com.clinicaviene.dto;

import java.time.Instant;

/**
 * Link de download recém-criado (aparece só aqui). O paciente abre o link (ou lê o QR no celular) e informa o CPF.
 *
 * @param emailEnviadoPara e-mail mascarado (quando enviado)
 */
public record EntregaDocumentoCriadaResponse(String link, String qrCode, Instant expiraEm, String emailEnviadoPara) {

    @Override
    public String toString() {
        return "EntregaDocumentoCriadaResponse[expiraEm=" + expiraEm + "]";
    }
}
