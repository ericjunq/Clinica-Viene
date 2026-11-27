package br.com.clinicaviene.util;

import br.com.clinicaviene.config.EnvioCodigoProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Regra de reenvio da D-073 (código 2FA por e-mail, recuperação de senha, convites): espera crescente entre
 * envios e, depois do máximo de envios na janela, só após o bloqueio.
 */
public final class RegraReenvio {

    private RegraReenvio() {}

    /** Quando o próximo envio é permitido, dado quantos já houve na janela e quando foi o último. */
    public static Instant liberadoEm(int enviosNaJanela, Instant ultimoEnvio, EnvioCodigoProperties regras) {
        if (enviosNaJanela >= regras.maximoEnvios()) {
            return ultimoEnvio.plus(regras.bloqueio());
        }
        List<Duration> esperas = regras.esperas();
        return ultimoEnvio.plus(esperas.get(Math.min(enviosNaJanela - 1, esperas.size() - 1)));
    }
}
