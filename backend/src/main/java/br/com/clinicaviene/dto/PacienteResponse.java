package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.ResponsavelLegal;
import br.com.clinicaviene.enums.Parentesco;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Ficha cadastral do paciente (sem dado clínico). CPFs mascarados. */
public record PacienteResponse(
        UUID id,
        String nomeCompleto,
        String cpfMascarado,
        LocalDate dataNascimento,
        int idade,
        boolean menorDeIdade,
        String email,
        String telefone,
        EnderecoDto endereco,
        Responsavel responsavel,
        boolean semCpf,
        Instant criadoEm,
        Instant atualizadoEm) {

    public record Responsavel(
            String nomeCompleto, String cpfMascarado, Parentesco parentesco, String telefone, String email) {}

    public static PacienteResponse from(Paciente p, ResponsavelLegal r, LocalDate hoje) {
        return new PacienteResponse(
                p.getId(),
                p.getNomeCompleto(),
                UsuarioResponse.mascararCpf(p.getCpf()),
                p.getDataNascimento(),
                p.idade(hoje),
                p.menorDeIdade(hoje),
                p.getEmail(),
                p.getTelefone(),
                p.endereco(),
                r == null
                        ? null
                        : new Responsavel(
                                r.getNomeCompleto(),
                                UsuarioResponse.mascararCpf(r.getCpf()),
                                r.getParentesco(),
                                r.getTelefone(),
                                r.getEmail()),
                p.getCpf() == null,
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }
}
