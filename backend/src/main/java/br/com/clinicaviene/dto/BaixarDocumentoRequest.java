package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CPF do paciente (ou do responsável, para menor sem CPF) para liberar o download (D-094). */
public record BaixarDocumentoRequest(@NotBlank @Size(max = 20) String cpf) {

    @Override
    public String toString() {
        return "BaixarDocumentoRequest[***]";
    }
}
