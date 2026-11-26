package br.com.clinicaviene.identidade.dto;

import java.time.Instant;

/** JWT de acesso emitido e quando expira. */
public record TokenAcessoDto(String valor, Instant expiraEm) {

    @Override
    public String toString() {
        return "TokenAcessoDto[expiraEm=" + expiraEm + "]";
    }
}
