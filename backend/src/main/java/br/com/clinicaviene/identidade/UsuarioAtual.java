package br.com.clinicaviene.identidade;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Usuário da requisição atual, lido do token de acesso (JWT: {@code sub} = id, claim {@code perfil}). API
 * pública do módulo: os outros módulos usam para saber quem está agindo.
 */
@Component
public class UsuarioAtual {

    static final String CLAIM_PERFIL = "perfil";

    public UsuarioAutenticado obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            return new UsuarioAutenticado(
                    UUID.fromString(jwt.getSubject()), Perfil.valueOf(jwt.getClaimAsString(CLAIM_PERFIL)));
        }
        throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado");
    }
}
