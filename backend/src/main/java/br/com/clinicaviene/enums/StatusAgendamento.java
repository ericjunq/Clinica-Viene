package br.com.clinicaviene.enums;

import java.util.Set;

/** Situação do agendamento (CONTEXTO §7). Cancelado e falta encerram; concluído também. */
public enum StatusAgendamento {
    AGENDADO,
    CONFIRMADO,
    PACIENTE_CHEGOU,
    EM_ATENDIMENTO,
    CONCLUIDO,
    FALTOU,
    CANCELADO;

    /** Transições permitidas (remarcar = cancelar + novo agendamento ligado ao anterior). */
    public boolean podeIrPara(StatusAgendamento novo) {
        return switch (this) {
            case AGENDADO -> Set.of(CONFIRMADO, PACIENTE_CHEGOU, FALTOU, CANCELADO).contains(novo);
            case CONFIRMADO -> Set.of(PACIENTE_CHEGOU, FALTOU, CANCELADO).contains(novo);
            case PACIENTE_CHEGOU -> Set.of(EM_ATENDIMENTO, CANCELADO).contains(novo);
            case EM_ATENDIMENTO -> novo == CONCLUIDO;
            case CONCLUIDO, FALTOU, CANCELADO -> false;
        };
    }

    /** Ainda pode ser remarcado ou ter os itens alterados. */
    public boolean emAberto() {
        return this == AGENDADO || this == CONFIRMADO;
    }
}
