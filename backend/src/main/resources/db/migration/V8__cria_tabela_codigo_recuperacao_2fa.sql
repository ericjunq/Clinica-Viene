-- Códigos de recuperação do 2FA (D-072): 10 por usuário, uso único, só o hash SHA-256 no banco.
CREATE TABLE codigo_recuperacao_2fa
(
    id          UUID PRIMARY KEY,
    usuario_id  UUID                     NOT NULL REFERENCES usuario (id),
    codigo_hash VARCHAR(64)              NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX codigo_recuperacao_2fa_usuario_idx ON codigo_recuperacao_2fa (usuario_id);
