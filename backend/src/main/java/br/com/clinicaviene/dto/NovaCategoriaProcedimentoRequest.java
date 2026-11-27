package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Nova categoria. {@code codigo} ausente = gerado do nome (ex.: "Clínica geral" → CLINICA_GERAL). */
public record NovaCategoriaProcedimentoRequest(
        @NotNull AreaAtuacao areaAtuacao, @Size(max = 40) String codigo, @NotBlank @Size(max = 80) String nome) {}
