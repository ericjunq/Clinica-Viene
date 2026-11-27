-- Termos LGPD versionados e decisões do paciente (CONTEXTO §11, D-075, D-089, D-093).

-- Versão publicada não muda nem some; rascunho pode ser editado.
CREATE FUNCTION rejeita_alteracao_publicado() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.status = 'PUBLICADO' THEN
        RAISE EXCEPTION '% publicado não pode ser alterado: % não permitido', TG_TABLE_NAME, TG_OP;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TABLE termo_versao
(
    id            UUID PRIMARY KEY,
    tipo          VARCHAR(40)              NOT NULL,
    versao        INT                      NOT NULL,
    titulo        VARCHAR(150)             NOT NULL,
    texto         TEXT                     NOT NULL,
    status        VARCHAR(15)              NOT NULL,
    criado_por    UUID REFERENCES usuario (id),
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_por UUID REFERENCES usuario (id),
    publicado_em  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT termo_versao_uk UNIQUE (tipo, versao),
    CONSTRAINT termo_versao_status_ck CHECK (status IN ('RASCUNHO', 'PUBLICADO'))
);

CREATE TRIGGER termo_versao_publicado_imutavel
    BEFORE UPDATE OR DELETE
    ON termo_versao
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao_publicado();

-- Cada decisão do paciente (aceitou, recusou, revogou), com as evidências. Somente inclusão.
CREATE TABLE aceite_termo
(
    id               UUID PRIMARY KEY,
    termo_versao_id  UUID                     NOT NULL REFERENCES termo_versao (id),
    paciente_id      UUID                     NOT NULL REFERENCES paciente (id),
    tipo             VARCHAR(40)              NOT NULL,
    decisao          VARCHAR(15)              NOT NULL,
    origem           VARCHAR(20)              NOT NULL,
    pelo_responsavel BOOLEAN                  NOT NULL,
    ip               VARCHAR(45),
    dispositivo      VARCHAR(300),
    registrado_por   UUID REFERENCES usuario (id),
    justificativa    TEXT,
    registrado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT aceite_termo_decisao_ck CHECK (decisao IN ('ACEITO', 'RECUSADO', 'REVOGADO'))
);

CREATE INDEX aceite_termo_paciente_idx ON aceite_termo (paciente_id, tipo, registrado_em);

CREATE TRIGGER aceite_termo_somente_inclusao
    BEFORE UPDATE OR DELETE
    ON aceite_termo
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao();
