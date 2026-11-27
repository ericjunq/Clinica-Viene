package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.StatusAgendamento;
import jakarta.validation.constraints.NotNull;

/** Confirmar, chegada (check-in), início/fim do atendimento ou falta. Cancelar tem rota própria (com motivo). */
public record AlterarStatusAgendamentoRequest(@NotNull StatusAgendamento status) {}
