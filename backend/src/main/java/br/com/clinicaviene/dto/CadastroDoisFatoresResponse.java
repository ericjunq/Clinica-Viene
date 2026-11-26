package br.com.clinicaviene.dto;

/**
 * Dados para cadastrar o app autenticador: o QR code (PNG em data URI) e, para digitar à mão, o segredo.
 * Mostrados uma única vez.
 */
public record CadastroDoisFatoresResponse(String segredo, String uriOtpAuth, String qrCode) {

    @Override
    public String toString() {
        return "CadastroDoisFatoresResponse[***]";
    }
}
