-- Agenda, parte 1 (CONTEXTO §7; D-022, D-106): consultórios, categorias de procedimento e horários dos profissionais.

CREATE TABLE consultorio
(
    id            UUID PRIMARY KEY,
    nome          VARCHAR(80)              NOT NULL,
    ativo         BOOLEAN                  NOT NULL,
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX consultorio_nome_uk ON consultorio (lower(nome));

-- Categoria de procedimento por área (ex.: ODONTOLOGIA/ORTODONTIA). O código é estável: o financeiro (Fase 5)
-- liga preço e repasse a ele.
CREATE TABLE categoria_procedimento
(
    id            UUID PRIMARY KEY,
    area_atuacao  VARCHAR(30)              NOT NULL,
    codigo        VARCHAR(40)              NOT NULL,
    nome          VARCHAR(80)              NOT NULL,
    ativa         BOOLEAN                  NOT NULL,
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT categoria_procedimento_codigo_uk UNIQUE (area_atuacao, codigo)
);

CREATE UNIQUE INDEX categoria_procedimento_nome_uk ON categoria_procedimento (area_atuacao, lower(nome));

-- Horário fixo semanal do profissional num consultório (dia da semana ISO: 1 = segunda … 7 = domingo),
-- em hora local da clínica. Encerrar = preencher vigencia_fim (o histórico fica).
CREATE TABLE disponibilidade_profissional
(
    id              UUID PRIMARY KEY,
    profissional_id UUID                     NOT NULL REFERENCES usuario (id),
    consultorio_id  UUID                     NOT NULL REFERENCES consultorio (id),
    dia_semana      SMALLINT                 NOT NULL,
    hora_inicio     TIME                     NOT NULL,
    hora_fim        TIME                     NOT NULL,
    vigencia_inicio DATE                     NOT NULL,
    vigencia_fim    DATE,
    criado_por      UUID                     NOT NULL REFERENCES usuario (id),
    criado_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT disponibilidade_dia_semana_ck CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT disponibilidade_horario_ck CHECK (hora_fim > hora_inicio),
    CONSTRAINT disponibilidade_vigencia_ck CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
);

CREATE INDEX disponibilidade_profissional_idx ON disponibilidade_profissional (profissional_id, dia_semana);

-- Exceção pontual: HORARIO_EXTRA (brecha fora da rotina, num consultório) ou AUSENCIA (folga, férias, feriado).
-- Cancelar = preencher cancelada_em (o histórico fica).
CREATE TABLE excecao_agenda
(
    id              UUID PRIMARY KEY,
    profissional_id UUID                     NOT NULL REFERENCES usuario (id),
    consultorio_id  UUID REFERENCES consultorio (id),
    tipo            VARCHAR(20)              NOT NULL,
    inicio          TIMESTAMP WITH TIME ZONE NOT NULL,
    fim             TIMESTAMP WITH TIME ZONE NOT NULL,
    motivo          VARCHAR(300),
    criado_por      UUID                     NOT NULL REFERENCES usuario (id),
    criado_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelada_por   UUID REFERENCES usuario (id),
    cancelada_em    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT excecao_agenda_periodo_ck CHECK (fim > inicio),
    CONSTRAINT excecao_agenda_extra_ck CHECK (tipo <> 'HORARIO_EXTRA' OR consultorio_id IS NOT NULL)
);

CREATE INDEX excecao_agenda_profissional_idx ON excecao_agenda (profissional_id, inicio);
