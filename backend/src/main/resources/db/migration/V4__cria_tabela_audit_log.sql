-- Log de auditoria somente-inclusão (D-006, D-076).
-- Nunca guardar dados pessoais em claro aqui: só ids, ação e metadados técnicos.
CREATE TABLE audit_log
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocorrido_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    usuario_id    UUID,
    acao          VARCHAR(80)              NOT NULL,
    entidade      VARCHAR(80),
    entidade_id   VARCHAR(80),
    ip            VARCHAR(45),
    justificativa TEXT
);

CREATE INDEX audit_log_usuario_idx ON audit_log (usuario_id, ocorrido_em);
CREATE INDEX audit_log_entidade_idx ON audit_log (entidade, entidade_id);
CREATE INDEX audit_log_ocorrido_em_idx ON audit_log (ocorrido_em);

-- A garantia de "somente inclusão" fica no banco, não só no código da aplicação.
CREATE FUNCTION audit_log_rejeita_alteracao() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'audit_log é somente inclusão: % não permitido', TG_OP;
END;
$$;

CREATE TRIGGER audit_log_sem_update_delete
    BEFORE UPDATE OR DELETE
    ON audit_log
    FOR EACH ROW
EXECUTE FUNCTION audit_log_rejeita_alteracao();

CREATE TRIGGER audit_log_sem_truncate
    BEFORE TRUNCATE
    ON audit_log
    FOR EACH STATEMENT
EXECUTE FUNCTION audit_log_rejeita_alteracao();
