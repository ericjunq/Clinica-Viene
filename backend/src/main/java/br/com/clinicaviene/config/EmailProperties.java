package br.com.clinicaviene.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** @param remetente endereço "De" dos e-mails do sistema */
@Validated
@ConfigurationProperties("clinicaviene.email")
public record EmailProperties(@NotBlank String remetente) {}
