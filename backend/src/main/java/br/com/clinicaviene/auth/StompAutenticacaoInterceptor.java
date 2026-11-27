package br.com.clinicaviene.auth;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Autenticação e autorização do STOMP (D-108). No CONNECT, o cabeçalho {@code Authorization: Bearer <token>} passa
 * pelas mesmas regras da API ({@link UsuarioJwtConverter}: usuário ativo, sessão completa). Só se pode assinar os
 * próprios avisos e, quem usa a agenda, o tópico da agenda. O cliente não envia mensagens.
 */
@Component
@RequiredArgsConstructor
public class StompAutenticacaoInterceptor implements ChannelInterceptor {

    public static final String AVISOS = "/user/queue/avisos";
    public static final String AGENDA = "/topic/agenda";
    private static final Set<String> PERFIS_DA_AGENDA = Set.of("ROLE_ADMIN", "ROLE_MASTER", "ROLE_PROFISSIONAL");

    private final JwtDecoder jwtDecoder;
    private final UsuarioJwtConverter conversorJwt;

    @Override
    public Message<?> preSend(Message<?> mensagem, MessageChannel canal) {
        StompHeaderAccessor acesso = MessageHeaderAccessor.getAccessor(mensagem, StompHeaderAccessor.class);
        if (acesso == null || acesso.getCommand() == null) {
            return mensagem;
        }
        StompCommand comando = acesso.getCommand();
        if (comando == StompCommand.CONNECT) {
            acesso.setUser(autenticar(acesso.getFirstNativeHeader("Authorization")));
        } else if (comando == StompCommand.SUBSCRIBE) {
            if (!(acesso.getUser() instanceof AbstractAuthenticationToken usuario)) {
                throw new AccessDeniedException("Não autenticado");
            }
            String destino = acesso.getDestination();
            boolean permitido = AVISOS.equals(destino)
                    || (AGENDA.equals(destino) && usuario.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(PERFIS_DA_AGENDA::contains));
            if (!permitido) {
                throw new AccessDeniedException("Assinatura não permitida");
            }
        } else if (comando == StompCommand.SEND) {
            throw new AccessDeniedException("O cliente não envia mensagens");
        }
        return mensagem;
    }

    private AbstractAuthenticationToken autenticar(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            throw new AccessDeniedException("Token ausente");
        }
        AbstractAuthenticationToken autenticacao;
        try {
            autenticacao = conversorJwt.convert(jwtDecoder.decode(cabecalho.substring("Bearer ".length())));
        } catch (JwtException | org.springframework.security.core.AuthenticationException e) {
            throw new AccessDeniedException("Token inválido");
        }
        boolean completa = autenticacao.getAuthorities().stream()
                .anyMatch(a -> UsuarioJwtConverter.SESSAO_COMPLETA.equals(a.getAuthority()));
        if (!completa) {
            throw new AccessDeniedException("Sessão incompleta");
        }
        return autenticacao;
    }
}
