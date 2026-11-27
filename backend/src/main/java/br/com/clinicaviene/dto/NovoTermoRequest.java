package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.TipoTermo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Novo rascunho de termo: {@code POST /api/v1/termos}. */
public record NovoTermoRequest(
        @NotNull TipoTermo tipo, @NotBlank @Size(max = 150) String titulo, @NotBlank @Size(max = 100_000) String texto) {}
