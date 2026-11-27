package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** {@code PATCH /api/v1/pacientes/{id}/contato}: só contato e endereço (D-092). */
public record AtualizarContatoPacienteRequest(
        @Email @Size(max = 254) String email, @Size(max = 20) String telefone, @Valid EnderecoDto endereco) {}
