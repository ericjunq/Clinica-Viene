-- Convite de cadastro (CONTEXTO §4.2, D-091): token de uso único (só o hash no banco).
-- LINK: celular do paciente (72 h, pode ir por e-mail); QUIOSQUE: aparelho da clínica, com documento conferido.
CREATE TABLE convite_cadastro
(
    id                      UUID PRIMARY KEY,
    paciente_id             UUID                     NOT NULL REFERENCES paciente (id),
    token_hash              VARCHAR(64)              NOT NULL,
    modo                    VARCHAR(15)              NOT NULL,
    criado_por              UUID                     NOT NULL REFERENCES usuario (id),
    criado_em               TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em               TIMESTAMP WITH TIME ZONE NOT NULL,
    enviado_por_email_em    TIMESTAMP WITH TIME ZONE,
    documento_conferido_por UUID REFERENCES usuario (id),
    usado_em                TIMESTAMP WITH TIME ZONE,
    cancelado_em            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT convite_cadastro_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT convite_cadastro_modo_ck CHECK (modo IN ('LINK', 'QUIOSQUE')),
    CONSTRAINT convite_cadastro_quiosque_ck CHECK (modo <> 'QUIOSQUE' OR documento_conferido_por IS NOT NULL)
);

CREATE INDEX convite_cadastro_paciente_idx ON convite_cadastro (paciente_id, criado_em);
