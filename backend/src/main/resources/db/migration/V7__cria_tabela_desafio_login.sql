-- Desafio do login em 2 passos (D-071): emitido após a senha correta, consumido pelo 2FA.
-- Só o hash SHA-256 do token fica no banco. Expira em minutos e morre após N códigos errados.
CREATE TABLE desafio_login
(
    id                    UUID PRIMARY KEY,
    usuario_id            UUID                     NOT NULL REFERENCES usuario (id),
    token_hash            VARCHAR(64)              NOT NULL,
    tentativas            INT                      NOT NULL,
    criado_em             TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em             TIMESTAMP WITH TIME ZONE NOT NULL,
    concluido_em          TIMESTAMP WITH TIME ZONE,
    -- Segredo TOTP gerado no cadastro do 2FA, ainda não confirmado (cifrado com AES-GCM).
    totp_segredo_pendente TEXT,
    CONSTRAINT desafio_login_token_hash_uk UNIQUE (token_hash)
);

CREATE INDEX desafio_login_usuario_idx ON desafio_login (usuario_id);
