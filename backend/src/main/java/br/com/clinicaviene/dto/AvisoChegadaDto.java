package br.com.clinicaviene.dto;

import java.time.Instant;
import java.util.UUID;

/** Aviso ao profissional: o paciente da consulta chegou (D-023). Sem dado clínico. */
public record AvisoChegadaDto(
        String tipo, UUID agendamentoId, String pacienteNome, String consultorioNome, Instant inicio, Instant chegadaEm) {

    public static final String PACIENTE_CHEGOU = "PACIENTE_CHEGOU";
}
