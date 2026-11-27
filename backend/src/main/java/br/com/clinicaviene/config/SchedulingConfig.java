package br.com.clinicaviene.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as tarefas agendadas (@Scheduled), como a limpeza do limite de tentativas. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
