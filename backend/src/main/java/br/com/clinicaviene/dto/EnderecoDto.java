package br.com.clinicaviene.dto;

import jakarta.validation.constraints.Size;

/** Endereço do paciente (todos os campos opcionais). */
public record EnderecoDto(
        @Size(max = 9) String cep,
        @Size(max = 150) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 80) String complemento,
        @Size(max = 80) String bairro,
        @Size(max = 80) String cidade,
        @Size(max = 2) String uf) {

    public static EnderecoDto vazio() {
        return new EnderecoDto(null, null, null, null, null, null, null);
    }
}
