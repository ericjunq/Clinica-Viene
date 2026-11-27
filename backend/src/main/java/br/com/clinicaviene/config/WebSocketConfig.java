package br.com.clinicaviene.config;

import br.com.clinicaviene.auth.StompAutenticacaoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Avisos em tempo real (D-023, D-108): STOMP sobre WebSocket em {@code /api/v1/ws} (mesmo caminho da API, para o
 * nginx encaminhar). A autenticação é no CONNECT do STOMP, com o mesmo token de acesso da API.
 *
 * <ul>
 *   <li>{@code /user/queue/avisos}: avisos pessoais (ex.: paciente chegou, para o profissional da consulta).
 *   <li>{@code /topic/agenda}: "a agenda do dia X mudou" — só a data, sem dado pessoal — para as telas recarregarem.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAutenticacaoInterceptor autenticacao;
    private final ConviteProperties conviteProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registro) {
        // Navegador: só a origem pública do frontend. Sem cabeçalho Origin (ex.: testes) o Spring trata como mesma origem.
        registro.addEndpoint("/api/v1/ws").setAllowedOrigins(conviteProperties.urlPublica().replaceAll("/+$", ""));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registro) {
        registro.enableSimpleBroker("/topic", "/queue");
        registro.setApplicationDestinationPrefixes("/app");
        registro.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registro) {
        registro.interceptors(autenticacao);
    }
}
