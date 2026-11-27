-- Documentos assinados e ficha de cadastro/anamnese (CONTEXTO §5, §9; D-088, D-090, D-102).
-- Tudo somente inclusão: o banco rejeita UPDATE/DELETE (correção = nova ficha).

-- PDF guardado no storage privado (cifrado pela aplicação); aqui só a chave e o hash SHA-256 do PDF.
CREATE TABLE documento
(
    id                  UUID PRIMARY KEY,
    tipo                VARCHAR(40)              NOT NULL,
    paciente_id         UUID                     NOT NULL REFERENCES paciente (id),
    chave_armazenamento VARCHAR(300)             NOT NULL,
    sha256              VARCHAR(64)              NOT NULL,
    tamanho_bytes       BIGINT                   NOT NULL,
    criado_em           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT documento_chave_armazenamento_uk UNIQUE (chave_armazenamento)
);

CREATE INDEX documento_paciente_idx ON documento (paciente_id, criado_em);

-- Assinatura de um documento, com a trilha de evidências (cifrada: IP, aparelho, como a identidade foi confirmada).
CREATE TABLE assinatura
(
    id                     UUID PRIMARY KEY,
    documento_id           UUID                     NOT NULL REFERENCES documento (id),
    provedor               VARCHAR(30)              NOT NULL,
    identificador_provedor VARCHAR(120),
    tipo_signatario        VARCHAR(30)              NOT NULL,
    nome_signatario        VARCHAR(150)             NOT NULL,
    metodo                 VARCHAR(30)              NOT NULL,
    evidencias             TEXT                     NOT NULL,
    assinado_em            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX assinatura_documento_idx ON assinatura (documento_id);

-- Ficha assinada: respostas e alertas cifrados, ligados à versão do modelo usada e ao convite (uso único).
CREATE TABLE ficha_anamnese
(
    id                 UUID PRIMARY KEY,
    paciente_id        UUID                     NOT NULL REFERENCES paciente (id),
    modelo_anamnese_id UUID                     NOT NULL REFERENCES modelo_anamnese (id),
    convite_id         UUID                     NOT NULL REFERENCES convite_cadastro (id),
    documento_id       UUID                     NOT NULL REFERENCES documento (id),
    origem             VARCHAR(20)              NOT NULL,
    pelo_responsavel   BOOLEAN                  NOT NULL,
    respostas          TEXT                     NOT NULL,
    alertas            TEXT                     NOT NULL,
    acompanhante       TEXT,
    criada_em          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ficha_anamnese_convite_uk UNIQUE (convite_id),
    CONSTRAINT ficha_anamnese_documento_uk UNIQUE (documento_id)
);

CREATE INDEX ficha_anamnese_paciente_idx ON ficha_anamnese (paciente_id, criada_em);

CREATE TRIGGER documento_somente_inclusao
    BEFORE UPDATE OR DELETE
    ON documento
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao();

CREATE TRIGGER assinatura_somente_inclusao
    BEFORE UPDATE OR DELETE
    ON assinatura
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao();

CREATE TRIGGER ficha_anamnese_somente_inclusao
    BEFORE UPDATE OR DELETE
    ON ficha_anamnese
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao();

-- Entrega do PDF ao paciente (D-094): link de uso limitado; a página pede o CPF antes de liberar o download.
CREATE TABLE entrega_documento
(
    id                 UUID PRIMARY KEY,
    documento_id       UUID                     NOT NULL REFERENCES documento (id),
    token_hash         VARCHAR(64)              NOT NULL,
    criada_em          TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em          TIMESTAMP WITH TIME ZONE NOT NULL,
    downloads          INT                      NOT NULL,
    tentativas_cpf     INT                      NOT NULL,
    ultimo_download_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT entrega_documento_token_hash_uk UNIQUE (token_hash)
);

CREATE INDEX entrega_documento_documento_idx ON entrega_documento (documento_id);
