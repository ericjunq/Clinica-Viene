package br.com.clinicaviene.auditoria;

/** Ações registradas no log de auditoria. Persistido como texto (nunca ordinal). */
public enum AcaoAuditoria {
    // Usuários (Fase 1)
    USUARIO_INICIAL_CRIADO,
    USUARIO_CRIADO,
    USUARIO_ATIVADO,
    USUARIO_DESATIVADO,
    USUARIO_PERFIL_ALTERADO,
    SENHA_ALTERADA,

    // Autenticação (Fase 1)
    LOGIN_SENHA_OK,
    LOGIN_FALHA,
    LOGIN_BLOQUEADO_TENTATIVAS,
    DOIS_FATORES_CADASTRADO,
    DOIS_FATORES_OK,
    DOIS_FATORES_FALHA,
    CODIGO_RECUPERACAO_USADO,
    CODIGO_EMAIL_ENVIADO,
    SESSAO_RENOVADA,
    SESSAO_REUSO_DETECTADO,
    LOGOUT
}
