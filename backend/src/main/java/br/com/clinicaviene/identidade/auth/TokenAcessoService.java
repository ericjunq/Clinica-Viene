package br.com.clinicaviene.identidade.auth;

import br.com.clinicaviene.identidade.dto.TokenAcessoDto;
import br.com.clinicaviene.identidade.entity.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite o JWT de acesso: {@code sub} = id do usuário, claim {@code perfil}, validade curta (D-071). */
@Component
public class TokenAcessoService {

    private final JwtEncoder codificador;
    private final AuthProperties propriedades;
    private final Clock relogio;

    TokenAcessoService(JwtEncoder codificador, AuthProperties propriedades, Clock relogio) {
        this.codificador = codificador;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public TokenAcessoDto emitir(Usuario usuario) {
        Instant agora = Instant.now(relogio);
        Instant expiraEm = agora.plus(propriedades.duracaoTokenAcesso());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(AuthProperties.EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim(UsuarioAutenticadoProvider.CLAIM_PERFIL, usuario.getPerfil().name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(JwtConfig.ID_CHAVE)
                .build();
        String valor = codificador
                .encode(JwtEncoderParameters.from(cabecalho, claims))
                .getTokenValue();
        return new TokenAcessoDto(valor, expiraEm);
    }
}
