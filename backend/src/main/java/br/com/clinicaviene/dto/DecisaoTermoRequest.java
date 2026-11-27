package br.com.clinicaviene.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Decisão do paciente sobre uma versão de termo que ele viu na tela (desmarcado = recusado). */
public record DecisaoTermoRequest(@NotNull UUID termoVersaoId, Boolean aceito) {

    public boolean aceitou() {
        return Boolean.TRUE.equals(aceito);
    }
}
