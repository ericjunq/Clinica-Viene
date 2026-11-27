package br.com.clinicaviene.dto;

/**
 * Novo link de download pedido pela recepção: {@code POST /api/v1/pacientes/{id}/documentos/{documentoId}/entregas}.
 *
 * @param enviarEmail {@code true} envia o link ao e-mail cadastrado; senão, o link/QR é só mostrado na tela
 */
public record NovaEntregaDocumentoRequest(Boolean enviarEmail) {

    public boolean deveEnviarEmail() {
        return Boolean.TRUE.equals(enviarEmail);
    }
}
