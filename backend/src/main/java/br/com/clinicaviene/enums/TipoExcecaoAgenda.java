package br.com.clinicaviene.enums;

/** Exceção pontual na agenda de um profissional (D-106). */
public enum TipoExcecaoAgenda {
    /** Brecha fora da rotina semanal (ex.: quinta das 8h às 11h), num consultório. */
    HORARIO_EXTRA,
    /** Folga, férias, feriado: o profissional não atende no período. */
    AUSENCIA
}
