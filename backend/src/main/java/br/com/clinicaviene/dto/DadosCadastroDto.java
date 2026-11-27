package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dados cadastrais que o paciente confere/corrige e completa no fluxo público (D-091). */
public record DadosCadastroDto(
        @NotBlank @Size(max = 150) String nomeCompleto,
        String cpf,
        @NotNull LocalDate dataNascimento,
        @Email @Size(max = 254) String email,
        @Size(max = 20) String telefone,
        @Valid EnderecoDto endereco,
        @Valid ResponsavelLegalDto responsavel) {

    @Override
    public String toString() {
        return "DadosCadastroDto[***]";
    }
}
