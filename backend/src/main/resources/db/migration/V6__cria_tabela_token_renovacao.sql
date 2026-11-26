-- Tokens de renovação de sessão (D-071): valor opaco, só o hash SHA-256 fica no banco.
-- Cada login abre uma "cadeia"; cada renovação marca o token como usado e cria o próximo da cadeia.
-- Reuso de um token já usado = possível roubo → a cadeia inteira é revogada.
CREATE TABLE token_renovacao
(
    id          UUID PRIMARY KEY,
    usuario_id  UUID                     NOT NULL REFERENCES usuario (id),
    cadeia_id   UUID                     NOT NULL,
    token_hash  VARCHAR(64)              NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em    TIMESTAMP WITH TIME ZONE,
    revogado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT token_renovacao_hash_uk UNIQUE (token_hash)
);

CREATE INDEX token_renovacao_usuario_idx ON token_renovacao (usuario_id);
CREATE INDEX token_renovacao_cadeia_idx ON token_renovacao (cadeia_id);
