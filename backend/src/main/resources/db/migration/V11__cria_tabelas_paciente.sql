-- Pacientes (CONTEXTO §4, D-052, D-053, D-091, D-092).
-- CPF cifrado (AES-GCM) + cpf_hash (HMAC-SHA256), único quando existir (menor pode não ter CPF).

-- Função genérica para tabelas somente-inclusão (histórico, fichas assinadas…).
CREATE FUNCTION rejeita_alteracao() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION '% é somente inclusão: % não permitido', TG_TABLE_NAME, TG_OP;
END;
$$;

CREATE TABLE paciente
(
    id                    UUID PRIMARY KEY,
    nome_completo         VARCHAR(150)             NOT NULL,
    cpf                   TEXT,
    cpf_hash              VARCHAR(64),
    data_nascimento       DATE                     NOT NULL,
    email                 VARCHAR(254),
    telefone              VARCHAR(20),
    cep                   VARCHAR(8),
    logradouro            VARCHAR(150),
    numero                VARCHAR(20),
    complemento           VARCHAR(80),
    bairro                VARCHAR(80),
    cidade                VARCHAR(80),
    uf                    VARCHAR(2),
    sem_cpf_justificativa TEXT,
    sem_cpf_aprovado_por  UUID REFERENCES usuario (id),
    sem_cpf_aprovado_em   TIMESTAMP WITH TIME ZONE,
    criado_por            UUID REFERENCES usuario (id),
    criado_em             TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em         TIMESTAMP WITH TIME ZONE NOT NULL,
    versao                BIGINT                   NOT NULL,
    CONSTRAINT paciente_cpf_ck CHECK ((cpf IS NULL) = (cpf_hash IS NULL)),
    CONSTRAINT paciente_contato_ck CHECK (email IS NOT NULL OR telefone IS NOT NULL)
);

CREATE UNIQUE INDEX paciente_cpf_hash_uk ON paciente (cpf_hash) WHERE cpf_hash IS NOT NULL;
CREATE INDEX paciente_nome_idx ON paciente (lower(nome_completo));
CREATE INDEX paciente_nascimento_idx ON paciente (data_nascimento);

-- Responsável legal (obrigatório para menor de 18 — LGPD art. 14). CPF sempre obrigatório e cifrado.
CREATE TABLE responsavel_legal
(
    id            UUID PRIMARY KEY,
    paciente_id   UUID                     NOT NULL REFERENCES paciente (id),
    nome_completo VARCHAR(150)             NOT NULL,
    cpf           TEXT                     NOT NULL,
    cpf_hash      VARCHAR(64)              NOT NULL,
    parentesco    VARCHAR(20)              NOT NULL,
    telefone      VARCHAR(20),
    email         VARCHAR(254),
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT responsavel_legal_paciente_uk UNIQUE (paciente_id)
);

CREATE INDEX responsavel_legal_cpf_hash_idx ON responsavel_legal (cpf_hash);

-- Histórico do cadastro (D-092): cada alteração guarda um retrato cifrado. Somente inclusão.
CREATE TABLE paciente_versao
(
    id               UUID PRIMARY KEY,
    paciente_id      UUID                     NOT NULL REFERENCES paciente (id),
    dados            TEXT                     NOT NULL,
    campos_alterados VARCHAR(300)             NOT NULL,
    origem           VARCHAR(20)              NOT NULL,
    alterado_por     UUID REFERENCES usuario (id),
    criado_em        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX paciente_versao_paciente_idx ON paciente_versao (paciente_id, criado_em);

CREATE TRIGGER paciente_versao_somente_inclusao
    BEFORE UPDATE OR DELETE
    ON paciente_versao
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao();
