package br.com.clinicaviene;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Relógio que os testes podem avançar, para testar expirações sem esperar. Sempre em UTC. */
public class RelogioAjustavel extends Clock {

    private volatile Instant agora;

    public RelogioAjustavel(Instant inicio) {
        this.agora = inicio;
    }

    public void avancar(Duration tempo) {
        agora = agora.plus(tempo);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }

    /** Substitui o relógio da aplicação nos testes que importarem esta configuração. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Configuracao {

        @Bean
        @Primary
        public RelogioAjustavel relogioAjustavel() {
            // Em microssegundos, como o PostgreSQL guarda.
            return new RelogioAjustavel(Instant.now().truncatedTo(ChronoUnit.MICROS));
        }
    }
}
