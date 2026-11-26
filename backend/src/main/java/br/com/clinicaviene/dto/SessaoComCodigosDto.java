package br.com.clinicaviene.dto;

import java.util.List;

/** Sessão aberta ao confirmar o cadastro do 2FA, junto dos códigos de recuperação (mostrados uma vez). */
public record SessaoComCodigosDto(SessaoDto sessao, List<String> codigosRecuperacao) {

    @Override
    public String toString() {
        return "SessaoComCodigosDto[" + sessao + "]";
    }
}
