package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.Parentesco;
import java.time.LocalDate;

/** Retrato completo do cadastro guardado (cifrado) em cada versão do histórico (D-092). Uso interno. */
public record PacienteRetratoDto(
        String nomeCompleto,
        String cpf,
        LocalDate dataNascimento,
        String email,
        String telefone,
        EnderecoDto endereco,
        Responsavel responsavel) {

    public record Responsavel(String nomeCompleto, String cpf, Parentesco parentesco, String telefone, String email) {}

    @Override
    public String toString() {
        return "PacienteRetratoDto[***]";
    }
}
