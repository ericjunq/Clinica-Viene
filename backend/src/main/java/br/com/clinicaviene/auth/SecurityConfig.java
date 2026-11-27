package br.com.clinicaviene.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança HTTP (D-071): tudo fechado por padrão, sem sessão no servidor, JWT no cabeçalho
 * {@code Authorization}. O {@link UsuarioJwtConverter} consulta o usuário a cada requisição.
 *
 * <ul>
 *   <li>Públicos: health, etapas do login, renovar e logout (estes dois usam o cookie HttpOnly).
 *   <li>Só autenticado (mesmo com troca de senha pendente): {@code /auth/eu} e a troca da própria senha.
 *   <li>Todo o resto: sessão completa ({@link UsuarioJwtConverter#SESSAO_COMPLETA}).
 * </ul>
 *
 * <p>CSRF desligado: a API autentica pelo cabeçalho {@code Authorization}, que o navegador não envia
 * sozinho; o cookie de renovação é {@code SameSite=Strict}, restrito a {@code /api/v1/auth} e a resposta da
 * renovação vem no corpo (ilegível por outro site).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadeiaDeSeguranca(
            HttpSecurity http,
            UsuarioJwtConverter conversorJwt,
            @Value("${clinicaviene.openapi.habilitado:false}") boolean openApiHabilitado)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt))
                        .authenticationEntryPoint(SecurityProblemHandler.naoAutenticado())
                        .accessDeniedHandler(SecurityProblemHandler.acessoNegado()))
                .exceptionHandling(erros -> erros.authenticationEntryPoint(SecurityProblemHandler.naoAutenticado())
                        .accessDeniedHandler(SecurityProblemHandler.acessoNegado()))
                .authorizeHttpRequests(rotas -> {
                    rotas.requestMatchers("/actuator/health", "/actuator/health/**")
                            .permitAll();
                    rotas.requestMatchers(
                                    HttpMethod.POST,
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/2fa/**",
                                    "/api/v1/auth/senha/**",
                                    "/api/v1/auth/renovar",
                                    "/api/v1/auth/logout")
                            .permitAll();
                    // Cadastro do paciente pelo convite (D-100): a autorização é o token do convite + sessão de
                    // preenchimento, conferidos no CadastroPublicoService.
                    rotas.requestMatchers("/api/v1/publico/**").permitAll();
                    if (openApiHabilitado) {
                        rotas.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll();
                    }
                    rotas.requestMatchers(HttpMethod.GET, "/api/v1/auth/eu").authenticated();
                    rotas.requestMatchers(HttpMethod.POST, "/api/v1/usuarios/eu/senha").authenticated();
                    rotas.anyRequest().hasAuthority(UsuarioJwtConverter.SESSAO_COMPLETA);
                });
        return http.build();
    }
}
