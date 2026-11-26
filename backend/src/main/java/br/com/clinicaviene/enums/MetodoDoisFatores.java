package br.com.clinicaviene.enums;

/** Como o usuário comprova o segundo fator (D-072). */
public enum MetodoDoisFatores {
    /** Código de 6 dígitos do app autenticador. */
    TOTP,
    /** Código de recuperação de uso único. */
    RECUPERACAO,
    /** Código de 6 dígitos enviado por e-mail (alternativa quando o usuário está sem o celular). */
    EMAIL
}
