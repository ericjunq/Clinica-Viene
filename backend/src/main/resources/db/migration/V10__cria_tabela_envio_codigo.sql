-- Códigos enviados por e-mail (2FA por e-mail, recuperação de senha) — D-072, D-073.
-- Guarda o histórico de envios para aplicar a espera entre reenvios e o limite por hora.
-- Só o hash SHA-256 do código fica no banco.
CREATE TABLE envio_codigo
(
    id            UUID PRIMARY KEY,
    usuario_id    UUID                     NOT NULL REFERENCES usuario (id),
    finalidade    VARCHAR(30)              NOT NULL,
    codigo_hash   VARCHAR(64)              NOT NULL,
    tentativas    INT                      NOT NULL,
    enviado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em      TIMESTAMP WITH TIME ZONE,
    invalidado_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX envio_codigo_usuario_finalidade_idx ON envio_codigo (usuario_id, finalidade, enviado_em);
