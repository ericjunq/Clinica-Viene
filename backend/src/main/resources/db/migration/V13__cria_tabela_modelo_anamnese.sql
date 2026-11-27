-- Modelos de anamnese versionados por área de atuação (CONTEXTO §5, D-070, D-089).
-- Perguntas em JSON. Versão publicada não muda nem some (fichas assinadas apontam para ela).
CREATE TABLE modelo_anamnese
(
    id            UUID PRIMARY KEY,
    area_atuacao  VARCHAR(30)              NOT NULL,
    versao        INT                      NOT NULL,
    titulo        VARCHAR(150)             NOT NULL,
    perguntas     TEXT                     NOT NULL,
    status        VARCHAR(15)              NOT NULL,
    criado_por    UUID REFERENCES usuario (id),
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_por UUID REFERENCES usuario (id),
    publicado_em  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT modelo_anamnese_uk UNIQUE (area_atuacao, versao),
    CONSTRAINT modelo_anamnese_status_ck CHECK (status IN ('RASCUNHO', 'PUBLICADO'))
);

CREATE TRIGGER modelo_anamnese_publicado_imutavel
    BEFORE UPDATE OR DELETE
    ON modelo_anamnese
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao_publicado();
