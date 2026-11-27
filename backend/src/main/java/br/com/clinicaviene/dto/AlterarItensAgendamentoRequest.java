package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Troca itens e observação de uma consulta ainda não atendida. */
public record AlterarItensAgendamentoRequest(
        @NotEmpty @Size(max = 10) List<@Valid @NotNull ItemAgendamentoRequest> itens, @Size(max = 500) String observacao) {}
