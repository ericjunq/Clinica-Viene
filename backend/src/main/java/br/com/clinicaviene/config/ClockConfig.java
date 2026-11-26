package br.com.clinicaviene.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** Relógio em UTC (datas no banco sempre em UTC). Injetável para os testes controlarem o tempo. */
    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
