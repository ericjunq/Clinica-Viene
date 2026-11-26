-- Último passo TOTP (janela de 30 s) aceito por usuário: impede reusar o mesmo código (anti-replay).
CREATE TABLE totp_uso
(
    usuario_id   UUID PRIMARY KEY REFERENCES usuario (id),
    ultimo_passo BIGINT NOT NULL
);
