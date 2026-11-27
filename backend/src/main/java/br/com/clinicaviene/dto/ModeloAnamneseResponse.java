package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.StatusVersao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ModeloAnamneseResponse(
        UUID id,
        AreaAtuacao areaAtuacao,
        int versao,
        String titulo,
        StatusVersao status,
        List<PerguntaAnamneseDto> perguntas,
        Instant criadoEm,
        Instant publicadoEm) {}
