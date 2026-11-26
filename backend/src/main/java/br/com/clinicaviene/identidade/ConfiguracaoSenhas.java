package br.com.clinicaviene.identidade;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class ConfiguracaoSenhas {

    /** Argon2id com os parâmetros recomendados pelo Spring Security (D-074). */
    @Bean
    PasswordEncoder codificadorDeSenhas() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
