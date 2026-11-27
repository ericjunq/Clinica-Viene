package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Agenda (D-106).
 *
 * @param grade de quanto em quanto tempo um horário pode começar (e o tamanho da linha no calendário)
 * @param duracaoPadrao duração que vem preenchida ao marcar (a recepção pode mudar)
 * @param fuso fuso da clínica: horários fixos e dias da semana são em hora local
 */
@Validated
@ConfigurationProperties("clinicaviene.agenda")
public record AgendaProperties(@NotNull Duration grade, @NotNull Duration duracaoPadrao, @NotNull ZoneId fuso) {}
