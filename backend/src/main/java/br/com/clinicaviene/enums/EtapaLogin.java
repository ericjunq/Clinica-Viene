package br.com.clinicaviene.enums;

/** Próxima etapa depois da senha aceita (D-071, D-072). */
public enum EtapaLogin {
    /** Primeiro acesso: cadastrar o app autenticador (QR code) antes de entrar. */
    CADASTRAR_2FA,
    /** 2FA já cadastrado: informar o código. */
    VERIFICAR_2FA
}
