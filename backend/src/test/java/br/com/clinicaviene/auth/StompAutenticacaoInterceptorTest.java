package br.com.clinicaviene.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Regras de assinatura do STOMP (D-108), sem servidor. */
class StompAutenticacaoInterceptorTest {

    private final StompAutenticacaoInterceptor interceptor = new StompAutenticacaoInterceptor(
            token -> {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("inválido");
            },
            null);

    private static JwtAuthenticationToken usuario(String perfil) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").issuedAt(Instant.now()).build();
        return new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + perfil), new SimpleGrantedAuthority("SESSAO_COMPLETA")), "u1");
    }

    private static Message<byte[]> quadro(StompCommand comando, String destino, JwtAuthenticationToken quem) {
        StompHeaderAccessor acesso = StompHeaderAccessor.create(comando);
        acesso.setDestination(destino);
        acesso.setUser(quem);
        acesso.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], acesso.getMessageHeaders());
    }

    @Test
    void cadaUmAssinaOsProprioAvisosEQuemUsaAAgendaAssinaOTopico() {
        assertThat(interceptor.preSend(quadro(StompCommand.SUBSCRIBE, "/user/queue/avisos", usuario("DEV")), null))
                .isNotNull();
        assertThat(interceptor.preSend(quadro(StompCommand.SUBSCRIBE, "/topic/agenda", usuario("ADMIN")), null))
                .isNotNull();
        assertThat(interceptor.preSend(quadro(StompCommand.SUBSCRIBE, "/topic/agenda", usuario("PROFISSIONAL")), null))
                .isNotNull();
    }

    @Test
    void recusaDevNaAgendaOutrosDestinosEnvioESemUsuario() {
        assertThatThrownBy(() -> interceptor.preSend(quadro(StompCommand.SUBSCRIBE, "/topic/agenda", usuario("DEV")), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(
                        quadro(StompCommand.SUBSCRIBE, "/user/outro-id/queue/avisos", usuario("ADMIN")), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(quadro(StompCommand.SEND, "/app/qualquer", usuario("ADMIN")), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(quadro(StompCommand.SUBSCRIBE, "/topic/agenda", null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connectSemTokenOuComTokenInvalidoERecusado() {
        assertThatThrownBy(() -> interceptor.preSend(quadro(StompCommand.CONNECT, null, null), null))
                .isInstanceOf(AccessDeniedException.class);
        StompHeaderAccessor acesso = StompHeaderAccessor.create(StompCommand.CONNECT);
        acesso.addNativeHeader("Authorization", "Bearer falso");
        acesso.setLeaveMutable(true);
        Message<byte[]> connect = MessageBuilder.createMessage(new byte[0], acesso.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(connect, null)).isInstanceOf(AccessDeniedException.class);
    }
}
