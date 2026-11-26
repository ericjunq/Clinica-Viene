package br.com.clinicaviene.compartilhado.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança base (Fase 1A): tudo fechado por padrão, sem sessão no servidor, autorização também nos
 * services via {@code @PreAuthorize} ({@link EnableMethodSecurity}). A autenticação por JWT entra na 1D.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain cadeiaDeSeguranca(
            HttpSecurity http, @Value("${clinicaviene.openapi.habilitado:false}") boolean openApiHabilitado)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(erros -> erros.authenticationEntryPoint(RespostaProblemaSeguranca.naoAutenticado())
                        .accessDeniedHandler(RespostaProblemaSeguranca.acessoNegado()))
                .authorizeHttpRequests(rotas -> {
                    rotas.requestMatchers("/actuator/health", "/actuator/health/**")
                            .permitAll();
                    if (openApiHabilitado) {
                        rotas.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll();
                    }
                    rotas.anyRequest().authenticated();
                });
        return http.build();
    }
}
