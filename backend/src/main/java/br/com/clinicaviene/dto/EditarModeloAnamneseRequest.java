package br.com.clinicaviene.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Edição de rascunho de modelo: {@code PUT /api/v1/modelos-anamnese/{id}}. */
public record EditarModeloAnamneseRequest(
        @NotBlank @Size(max = 150) String titulo, @NotEmpty @Size(max = 300) List<@Valid PerguntaAnamneseDto> perguntas) {}
