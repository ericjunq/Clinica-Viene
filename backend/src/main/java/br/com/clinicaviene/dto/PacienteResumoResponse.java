package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Paciente;
import java.time.LocalDate;
import java.util.UUID;

/** Linha da busca de pacientes. */
public record PacienteResumoResponse(
        UUID id, String nomeCompleto, String cpfMascarado, LocalDate dataNascimento, String telefone) {

    public static PacienteResumoResponse from(Paciente p) {
        return new PacienteResumoResponse(
                p.getId(), p.getNomeCompleto(), UsuarioResponse.mascararCpf(p.getCpf()), p.getDataNascimento(), p.getTelefone());
    }
}
