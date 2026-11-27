package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Novo rascunho de modelo de anamnese: {@code POST /api/v1/modelos-anamnese}. */
public record NovoModeloAnamneseRequest(
        @NotNull AreaAtuacao areaAtuacao,
        @NotBlank @Size(max = 150) String titulo,
        @NotEmpty @Size(max = 300) List<@Valid PerguntaAnamneseDto> perguntas) {}
