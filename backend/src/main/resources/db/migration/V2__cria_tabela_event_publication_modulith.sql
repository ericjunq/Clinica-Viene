-- Registro de publicação de eventos do Spring Modulith (spring-modulith-starter-jpa) — D-049.
-- Schema v2 do Spring Modulith 2.2.0 para PostgreSQL. Como ddl-auto=validate, a tabela precisa vir do Flyway.
-- Eventos devem carregar apenas ids, nunca dados pessoais (o evento fica serializado aqui).
CREATE TABLE event_publication
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

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
