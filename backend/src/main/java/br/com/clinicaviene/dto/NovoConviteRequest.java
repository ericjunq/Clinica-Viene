package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.ModoConvite;
import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /api/v1/pacientes/{id}/convites}.
 *
 * @param enviarEmail (LINK) envia o link para o e-mail do paciente — ou do responsável, se menor
 * @param documentoConferido (QUIOSQUE) a recepcionista conferiu o documento com foto do paciente
 */
public record NovoConviteRequest(@NotNull ModoConvite modo, Boolean enviarEmail, Boolean documentoConferido) {

    public boolean deveEnviarEmail() {
        return Boolean.TRUE.equals(enviarEmail);
    }

    public boolean documentoFoiConferido() {
        return Boolean.TRUE.equals(documentoConferido);
    }
}
