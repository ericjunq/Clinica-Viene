package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ModoConvite;
import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /api/v1/pacientes/{id}/convites}.
 *
 * @param enviarEmail (LINK) envia o link para o e-mail do paciente — ou do responsável, se menor
 * @param documentoConferido (QUIOSQUE) a recepcionista conferiu o documento com foto do paciente
 * @param areaAtuacao área da ficha de saúde (opcional: sem ela, a área padrão — D-101)
 */
public record NovoConviteRequest(
        @NotNull ModoConvite modo, Boolean enviarEmail, Boolean documentoConferido, AreaAtuacao areaAtuacao) {

    public boolean deveEnviarEmail() {
        return Boolean.TRUE.equals(enviarEmail);
    }

    public boolean documentoFoiConferido() {
        return Boolean.TRUE.equals(documentoConferido);
    }
}
