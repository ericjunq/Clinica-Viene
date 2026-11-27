package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.CategoriaProcedimento;
import br.com.clinicaviene.enums.AreaAtuacao;
import java.util.UUID;

public record CategoriaProcedimentoResponse(UUID id, AreaAtuacao areaAtuacao, String codigo, String nome, boolean ativa) {

    public static CategoriaProcedimentoResponse from(CategoriaProcedimento c) {
        return new CategoriaProcedimentoResponse(c.getId(), c.getAreaAtuacao(), c.getCodigo(), c.getNome(), c.isAtiva());
    }
}
