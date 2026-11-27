package br.com.clinicaviene.auth;

import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.enums.Perfil;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Usuário da requisição atual. O id vem do token ({@code sub}); o <b>perfil vem do banco</b>, carregado a cada
 * requisição pelo {@link UsuarioJwtConverter} (autoridade {@code ROLE_<perfil>}) — assim uma mudança de perfil
 * vale na hora, sem esperar o token expirar. O claim {@code perfil} do JWT é só informativo.
 */
@Component
public class UsuarioAutenticadoProvider {

    public static final String CLAIM_PERFIL = "perfil";
    private static final String PREFIXO_PERFIL = "ROLE_";

    public UsuarioAutenticado obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            Perfil perfil = token.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith(PREFIXO_PERFIL))
                    .map(a -> Perfil.valueOf(a.substring(PREFIXO_PERFIL.length())))
                    .findFirst()
                    .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuário sem perfil"));
            return new UsuarioAutenticado(UUID.fromString(token.getToken().getSubject()), perfil);
        }
        throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado");
    }
}
