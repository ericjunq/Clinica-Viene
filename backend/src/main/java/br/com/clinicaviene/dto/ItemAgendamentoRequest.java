package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Item do agendamento: categoria (da área do profissional) + descrição opcional. */
public record ItemAgendamentoRequest(@NotNull UUID categoriaId, @Size(max = 300) String descricao) {}
