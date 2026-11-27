package br.com.clinicaviene.dto;

import java.util.List;

/** Horários fixos em vigor (ou futuros) e exceções ativas a partir de hoje. */
public record HorariosProfissionalResponse(
        ProfissionalResumoResponse profissional, List<HorarioFixoResponse> fixos, List<ExcecaoAgendaResponse> excecoes) {}
