-- Agenda: confirmação de leitura do aviso de chegada (D-108, D-111). O profissional da consulta clica em "Ciente"
-- no aviso em tempo real e isso fica registrado (somente inclusão; um registro por consulta).

CREATE TABLE aviso_chegada_ciente
(
    id              UUID PRIMARY KEY,
    agendamento_id  UUID                     NOT NULL REFERENCES agendamento (id),
    profissional_id UUID                     NOT NULL REFERENCES usuario (id),
    ciente_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT aviso_chegada_ciente_agendamento_uk UNIQUE (agendamento_id)
);

CREATE TRIGGER aviso_chegada_ciente_somente_inclusao
    BEFORE UPDATE OR DELETE
    ON aviso_chegada_ciente
    FOR EACH ROW
EXECUTE FUNCTION rejeita_alteracao();
