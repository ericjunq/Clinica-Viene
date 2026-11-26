-- Arquivo de eventos concluídos do Spring Modulith (completion-mode=archive) — D-059.
-- Schema v2 do Spring Modulith 2.2.0 para PostgreSQL. Como ddl-auto=validate, a tabela precisa vir do Flyway.
-- Eventos carregam apenas ids, nunca dados pessoais. Limpeza periódica: ver CHECKLIST.
CREATE TABLE event_publication_archive
(
    id                     UUID                     NOT NULL,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_archive_serialized_event_hash_idx ON event_publication_archive USING hash (serialized_event);
CREATE INDEX event_publication_archive_by_completion_date_idx ON event_publication_archive (completion_date);
