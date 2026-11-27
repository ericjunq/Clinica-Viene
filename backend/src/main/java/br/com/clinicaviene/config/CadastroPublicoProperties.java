package br.com.clinicaviene.config;

import br.com.clinicaviene.enums.AreaAtuacao;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Preenchimento do cadastro pelo paciente (D-100).
 *
 * @param validadeSessao quanto tempo o paciente tem para preencher depois de confirmar a identidade
 * @param areaAnamnese área do modelo de anamnese usado no cadastro (enquanto o convite não escolhe a área)
 */
@Validated
@ConfigurationProperties("clinicaviene.cadastro-publico")
public record CadastroPublicoProperties(@NotNull Duration validadeSessao, @NotNull AreaAtuacao areaAnamnese) {}
