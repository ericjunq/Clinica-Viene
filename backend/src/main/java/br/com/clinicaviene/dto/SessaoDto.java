package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Usuario;
import java.time.Instant;

/**
 * Sessão emitida (D-071). {@code tokenRenovacao} vai só no cookie HttpOnly, nunca no corpo da resposta.
 */
public record SessaoDto(
        String tokenAcesso, Instant acessoExpiraEm, String tokenRenovacao, Instant sessaoExpiraEm, Usuario usuario) {

    @Override
    public String toString() {
        // Nunca expor tokens em logs.
        return "SessaoDto[usuario=" + usuario.getId() + "]";
    }
}
