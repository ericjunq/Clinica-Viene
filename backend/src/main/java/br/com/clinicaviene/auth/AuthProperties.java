package br.com.clinicaviene.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração da autenticação (D-071).
 *
 * @param chavePrivadaJwt chave RSA privada (PKCS#8 em Base64, uma linha; PEM também é aceito)
 * @param duracaoTokenAcesso validade do JWT de acesso
 * @param duracaoSessao validade máxima de uma sessão (cadeia de tokens de renovação), contada do login
 * @param toleranciaRenovacaoConcorrente janela em que reusar um token recém-trocado é tratado como corrida
 *     entre abas (resposta 409, sem revogar a sessão)
 * @param duracaoDesafio validade do desafio entre a senha e o 2FA
 * @param tentativasDesafio códigos 2FA errados permitidos por desafio
 * @param cookieSeguro marca o cookie de renovação como {@code Secure}
 * @param limiteLoginPorIp tentativas de login por IP dentro da janela
 * @param limiteFalhasPorCpf senhas erradas por CPF dentro da janela
 * @param janelaLimiteTentativas janela dos limites de tentativas
 */
@Validated
@ConfigurationProperties("clinicaviene.auth")
public record AuthProperties(
        @NotBlank(message = "defina JWT_CHAVE_PRIVADA") String chavePrivadaJwt,
        @NotNull Duration duracaoTokenAcesso,
        @NotNull Duration duracaoSessao,
        @NotNull Duration toleranciaRenovacaoConcorrente,
        @NotNull Duration duracaoDesafio,
        @Min(1) int tentativasDesafio,
        boolean cookieSeguro,
        @Min(1) int limiteLoginPorIp,
        @Min(1) int limiteFalhasPorCpf,
        @NotNull Duration janelaLimiteTentativas) {

    public static final String EMISSOR = "clinica-viene";

    @Override
    public String toString() {
        return "AuthProperties[duracaoTokenAcesso=" + duracaoTokenAcesso + ", duracaoSessao="
                + duracaoSessao + "]";
    }
}
