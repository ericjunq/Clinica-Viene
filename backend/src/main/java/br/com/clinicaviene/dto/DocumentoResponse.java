package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.enums.TipoDocumento;
import br.com.clinicaviene.enums.TipoSignatario;
import java.time.Instant;
import java.util.UUID;

/**
 * Documento do paciente na tela interna: só metadados. O conteúdo (que pode ter dado de saúde) nunca é mostrado à
 * recepção — ela só gera o link para o paciente baixar com o CPF (D-088, D-094).
 */
public record DocumentoResponse(
        UUID id,
        TipoDocumento tipo,
        Instant criadoEm,
        TipoSignatario assinadoPor,
        String nomeSignatario,
        MetodoAssinatura metodo) {}
