package br.com.clinicaviene.dto;

import java.time.Instant;

/** Token da sessão de preenchimento (enviado depois no cabeçalho {@code X-Sessao-Cadastro}). */
public record SessaoCadastroResponse(String token, Instant expiraEm) {

    @Override
    public String toString() {
        return "SessaoCadastroResponse[expiraEm=" + expiraEm + "]";
    }
}
