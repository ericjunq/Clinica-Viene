package br.com.clinicaviene.enums;

/** Para que um código foi enviado por e-mail. Cada finalidade tem seu próprio limite de reenvio (D-073). */
public enum FinalidadeEnvio {
    /** Código de 6 dígitos como alternativa ao app autenticador (D-072). */
    DOIS_FATORES_EMAIL,
    /** Código para redefinir a senha esquecida. */
    RECUPERACAO_SENHA
}
