package br.com.clinicaviene.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resposta de login concluído e de renovação. O token de renovação NÃO vem aqui: vai no cookie HttpOnly.
 *
 * @param codigosRecuperacao só no cadastro do 2FA (mostrados uma única vez); {@code null} nos demais casos
 */
public record SessaoResponse(
        String tokenAcesso,
        Instant acessoExpiraEm,
        Instant sessaoExpiraEm,
        UsuarioResponse usuario,
        List<String> codigosRecuperacao) {

    public static SessaoResponse from(SessaoDto sessao, List<String> codigosRecuperacao) {
        return new SessaoResponse(
                sessao.tokenAcesso(),
                sessao.acessoExpiraEm(),
                sessao.sessaoExpiraEm(),
                UsuarioResponse.from(sessao.usuario()),
                codigosRecuperacao);
    }

    @Override
    public String toString() {
        return "SessaoResponse[usuario=" + usuario.id() + "]";
    }
}
