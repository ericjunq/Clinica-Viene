package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.ModoConvite;
import java.time.Instant;
import java.util.UUID;

/**
 * Convite recém-criado. O link (com o token) e o QR code só aparecem aqui, uma única vez.
 *
 * @param emailEnviadoPara e-mail mascarado (quando enviado)
 * @param podeReenviarEm a partir de quando um novo envio por e-mail é permitido (D-073)
 */
public record ConviteCriadoResponse(
        UUID id,
        ModoConvite modo,
        String link,
        String qrCode,
        Instant expiraEm,
        String emailEnviadoPara,
        Instant podeReenviarEm) {

    @Override
    public String toString() {
        return "ConviteCriadoResponse[id=" + id + ", modo=" + modo + "]";
    }
}
