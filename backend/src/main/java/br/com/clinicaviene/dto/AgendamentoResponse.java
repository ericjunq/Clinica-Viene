package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.StatusAgendamento;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consulta na agenda. {@code paciente.fichaPendente}: ainda sem ficha de cadastro assinada (D-106) — a recepção
 * manda o convite ou abre o quiosque na chegada. {@code avisoCienteEm}: quando o profissional confirmou que viu o aviso
 * de chegada (D-111), ou nulo.
 */
public record AgendamentoResponse(
        UUID id,
        Paciente paciente,
        Referencia profissional,
        Referencia consultorio,
        Instant inicio,
        Instant fim,
        StatusAgendamento status,
        String observacao,
        List<Item> itens,
        UUID agendamentoOrigemId,
        String motivoCancelamento,
        Instant chegadaEm,
        Instant avisoCienteEm,
        Instant criadoEm) {

    public record Paciente(UUID id, String nomeCompleto, boolean fichaPendente) {}

    public record Referencia(UUID id, String nome) {}

    public record Item(UUID categoriaId, String categoriaCodigo, String categoriaNome, String descricao) {}
}
