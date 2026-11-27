-- Sessão de preenchimento do cadastro pelo paciente (fluxo público — CONTEXTO §4.2, D-091, D-100).
-- LINK: nasce com o código enviado ao e-mail e só vira sessão (token_hash) depois do código confirmado.
-- QUIOSQUE: nasce já confirmada (documento conferido pela recepção ao gerar o convite).
-- Só hashes SHA-256 do código e do token ficam no banco.
CREATE TABLE sessao_cadastro
(
    id                UUID PRIMARY KEY,
    convite_id        UUID                     NOT NULL REFERENCES convite_cadastro (id),
    codigo_hash       VARCHAR(64),
    codigo_expira_em  TIMESTAMP WITH TIME ZONE,
    tentativas_codigo INT                      NOT NULL,
    codigo_enviado_em TIMESTAMP WITH TIME ZONE,
    token_hash        VARCHAR(64),
    criada_em         TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmada_em     TIMESTAMP WITH TIME ZONE,
    expira_em         TIMESTAMP WITH TIME ZONE,
    ip                VARCHAR(45),
    dispositivo       VARCHAR(300),
    CONSTRAINT sessao_cadastro_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT sessao_cadastro_confirmada_ck CHECK ((confirmada_em IS NULL) = (token_hash IS NULL))
);

CREATE INDEX sessao_cadastro_convite_idx ON sessao_cadastro (convite_id, criada_em);
