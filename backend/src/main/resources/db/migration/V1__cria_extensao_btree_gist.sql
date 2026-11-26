-- Extensão necessária para impedir conflitos de agenda no banco
-- (EXCLUDE USING gist com tstzrange + igualdade de profissional/consultório) — D-021, D-050.
-- Criada por migration (e não por script de init do Docker) para existir em todos os ambientes.
CREATE EXTENSION IF NOT EXISTS btree_gist;
