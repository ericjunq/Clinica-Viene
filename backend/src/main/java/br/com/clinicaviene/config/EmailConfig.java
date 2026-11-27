package br.com.clinicaviene.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({EmailProperties.class, EnvioCodigoProperties.class, ConviteProperties.class, CadastroPublicoProperties.class})
public class EmailConfig {}
