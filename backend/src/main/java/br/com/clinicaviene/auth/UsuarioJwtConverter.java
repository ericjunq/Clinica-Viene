package br.com.clinicaviene.auth;

import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Transforma o JWT validado em autenticação, consultando o usuário no banco a cada requisição (D-068):
 * inativo perde o acesso na hora, mesmo com token ainda válido. Usuário com troca de senha pendente só
 * recebe acesso às rotas de troca de senha (sem a autoridade {@link #SESSAO_COMPLETA}).
 */
@Component
public class UsuarioJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String SESSAO_COMPLETA = "SESSAO_COMPLETA";

    private final UsuarioRepository repositorio;

    UsuarioJwtConverter(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DisabledException("Token sem usuário válido");
        }
        Usuario usuario = repositorio
                .findById(id)
                .filter(Usuario::ativo)
                .orElseThrow(() -> new DisabledException("Usuário inexistente ou inativo"));
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
        if (!usuario.isTrocarSenha()) {
            autoridades.add(new SimpleGrantedAuthority(SESSAO_COMPLETA));
        }
        return new JwtAuthenticationToken(jwt, autoridades, id.toString());
    }
}
