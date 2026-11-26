package br.com.clinicaviene.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dados do usuário DEV criado pelo inicializador (D-048), vindos do ambiente: DEV_INICIAL_CPF,
 * DEV_INICIAL_NOME, DEV_INICIAL_EMAIL, DEV_INICIAL_SENHA. Só são usados com o banco sem usuários; depois
 * disso podem (e devem) ser removidos do ambiente.
 */
@ConfigurationProperties("clinicaviene.dev-inicial")
public record DevInicialProperties(String cpf, String nome, String email, String senha) {

    boolean completas() {
        return preenchido(cpf) && preenchido(nome) && preenchido(email) && preenchido(senha);
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public String toString() {
        return "DevInicialProperties[completas=" + completas() + "]";
    }
}
