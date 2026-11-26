package br.com.clinicaviene.compartilhado.seguranca;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Respostas 401/403 do Spring Security no formato Problem Details (RFC 9457). */
public final class RespostaProblemaSeguranca {

    private RespostaProblemaSeguranca() {}

    public static AuthenticationEntryPoint naoAutenticado() {
        return (requisicao, resposta, excecao) -> escrever(resposta, 401, "Unauthorized", "Autenticação necessária.");
    }

    public static AccessDeniedHandler acessoNegado() {
        return (requisicao, resposta, excecao) -> escrever(resposta, 403, "Forbidden", "Acesso negado.");
    }

    private static void escrever(HttpServletResponse resposta, int status, String titulo, String detalhe)
            throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter()
                .write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + status
                        + ",\"detail\":\"" + detalhe + "\"}");
    }
}
