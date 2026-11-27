-- Agenda, parte 2 (CONTEXTO §7; D-020, D-021, D-106): agendamentos e seus itens (categoria + descrição).

CREATE TABLE agendamento
(
    id                    UUID PRIMARY KEY,
    paciente_id           UUID                     NOT NULL REFERENCES paciente (id),
    profissional_id       UUID                     NOT NULL REFERENCES usuario (id),
    consultorio_id        UUID                     NOT NULL REFERENCES consultorio (id),
    inicio                TIMESTAMP WITH TIME ZONE NOT NULL,
    fim                   TIMESTAMP WITH TIME ZONE NOT NULL,
    status                VARCHAR(20)              NOT NULL,
    observacao            VARCHAR(500),
    agendamento_origem_id UUID REFERENCES agendamento (id),
    motivo_cancelamento   VARCHAR(300),
    cancelado_por         UUID REFERENCES usuario (id),
    cancelado_em          TIMESTAMP WITH TIME ZONE,
    chegada_em            TIMESTAMP WITH TIME ZONE,
    atendimento_inicio_em TIMESTAMP WITH TIME ZONE,
    atendimento_fim_em    TIMESTAMP WITH TIME ZONE,
    criado_por            UUID                     NOT NULL REFERENCES usuario (id),
    criado_em             TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em         TIMESTAMP WITH TIME ZONE NOT NULL,
    versao                BIGINT                   NOT NULL,
    CONSTRAINT agendamento_periodo_ck CHECK (fim > inicio),
    -- D-021: duas pessoas marcando ao mesmo tempo nunca ocupam o mesmo horário (cancelados não contam).
    CONSTRAINT agendamento_profissional_sem_conflito EXCLUDE USING gist (
        profissional_id WITH =, tstzrange(inicio, fim) WITH &&) WHERE (status <> 'CANCELADO'),
    CONSTRAINT agendamento_consultorio_sem_conflito EXCLUDE USING gist (
        consultorio_id WITH =, tstzrange(inicio, fim) WITH &&) WHERE (status <> 'CANCELADO')
);

CREATE INDEX agendamento_inicio_idx ON agendamento (inicio);
CREATE INDEX agendamento_paciente_idx ON agendamento (paciente_id, inicio);

-- Itens do agendamento: cada um vira um procedimento separado no prontuário e no financeiro (D-106).
CREATE TABLE agendamento_item
(
    id             UUID PRIMARY KEY,
    agendamento_id UUID         NOT NULL REFERENCES agendamento (id),
    ordem          SMALLINT     NOT NULL,
    categoria_id   UUID         NOT NULL REFERENCES categoria_procedimento (id),
    descricao      VARCHAR(300),
    CONSTRAINT agendamento_item_ordem_uk UNIQUE (agendamento_id, ordem)
);
