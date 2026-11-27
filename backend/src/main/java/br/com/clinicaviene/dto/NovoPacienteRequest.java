package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Pré-cadastro pela recepção (D-091): {@code POST /api/v1/pacientes}.
 *
 * @param semCpfJustificativa obrigatória para adulto sem CPF (e só o MASTER aprova — D-053)
 * @param confirmarDuplicidade {@code true} para criar mesmo com alerta de possível duplicidade
 */
public record NovoPacienteRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        String cpf,
        @NotNull LocalDate dataNascimento,
        @Email @Size(max = 254) String email,
        @Size(max = 20) String telefone,
        @Valid EnderecoDto endereco,
        @Valid ResponsavelLegalDto responsavel,
        @Size(max = 500) String semCpfJustificativa,
        boolean confirmarDuplicidade) {

    @Override
    public String toString() {
        return "NovoPacienteRequest[***]";
    }
}
