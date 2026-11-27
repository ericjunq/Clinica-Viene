package br.com.clinicaviene.dto;

import java.time.LocalDate;

/** "A agenda deste dia mudou": só a data (sem dado pessoal), para as telas abertas recarregarem. */
public record AgendaAlteradaDto(LocalDate dia) {}
