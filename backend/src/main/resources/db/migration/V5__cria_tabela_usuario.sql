-- Usuários internos (D-061, D-067, D-068, D-069).
-- CPF cifrado (AES-GCM) + cpf_hash (HMAC-SHA256) para unicidade e busca no login.
CREATE TABLE usuario
(
    id              UUID PRIMARY KEY,
    nome_completo   VARCHAR(150)             NOT NULL,
    cpf             TEXT                     NOT NULL,
    cpf_hash        VARCHAR(64)              NOT NULL,
    email           VARCHAR(254)             NOT NULL,
    telefone        VARCHAR(20),
    perfil          VARCHAR(20)              NOT NULL,
    status          VARCHAR(10)              NOT NULL,
    area_atuacao    VARCHAR(30),
    conselho        VARCHAR(10),
    numero_registro VARCHAR(20),
    uf_registro     VARCHAR(2),
    senha_hash      VARCHAR(255)             NOT NULL,
    trocar_senha    BOOLEAN                  NOT NULL,
    totp_segredo    TEXT,
    totp_ativo      BOOLEAN                  NOT NULL,
    criado_por      UUID REFERENCES usuario (id),
    criado_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    versao          BIGINT                   NOT NULL,
    CONSTRAINT usuario_cpf_hash_uk UNIQUE (cpf_hash),
    CONSTRAINT usuario_perfil_ck CHECK (perfil IN ('DEV', 'MASTER', 'ADMIN', 'PROFISSIONAL')),
    CONSTRAINT usuario_status_ck CHECK (status IN ('ATIVO', 'INATIVO')),
    -- Registro profissional obrigatório para PROFISSIONAL e ausente nos demais perfis.
    CONSTRAINT usuario_registro_profissional_ck CHECK (
        (perfil = 'PROFISSIONAL' AND area_atuacao IS NOT NULL AND conselho IS NOT NULL
            AND numero_registro IS NOT NULL AND uf_registro IS NOT NULL)
        OR
        (perfil <> 'PROFISSIONAL' AND area_atuacao IS NULL AND conselho IS NULL
            AND numero_registro IS NULL AND uf_registro IS NULL)
        )
);

CREATE UNIQUE INDEX usuario_email_uk ON usuario (lower(email));
CREATE UNIQUE INDEX usuario_registro_profissional_uk ON usuario (conselho, numero_registro, uf_registro)
    WHERE numero_registro IS NOT NULL;
