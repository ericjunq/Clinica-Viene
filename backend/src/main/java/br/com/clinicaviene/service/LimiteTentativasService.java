package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Limite de tentativas de login (Bucket4j, em memória — uma instância da aplicação):
 *
 * <ul>
 *   <li>por IP: toda tentativa conta;
 *   <li>por CPF: só as falhas contam (protege uma conta específica de força bruta vinda de vários IPs).
 * </ul>
 *
 * <p>Baldes cheios (sem tentativas recentes) são removidos periodicamente, para a memória não crescer com
 * IPs/CPFs que já não estão tentando.
 */
@Service
@RequiredArgsConstructor
public class LimiteTentativasService {

    private final AuthProperties propriedades;
    private final Clock clock;
    private final Map<String, Bucket> porIp = new ConcurrentHashMap<>();
    private final Map<String, Bucket> falhasPorCpf = new ConcurrentHashMap<>();
    private final Map<String, Bucket> cadastroPublicoPorIp = new ConcurrentHashMap<>();

    /** Consome uma tentativa do IP e confere se o CPF ainda pode tentar. */
    public void verificarLogin(String ip, String cpfHash) {
        Bucket doIp = porIp.computeIfAbsent(ip == null ? "desconhecido" : ip, k -> novo(propriedades.limiteLoginPorIp()));
        ConsumptionProbe consumo = doIp.tryConsumeAndReturnRemaining(1);
        if (!consumo.isConsumed()) {
            throw new MuitasTentativasException(TimeUnit.NANOSECONDS.toSeconds(consumo.getNanosToWaitForRefill()));
        }
        Bucket doCpf = cpfHash == null ? null : falhasPorCpf.get(cpfHash);
        if (doCpf != null && doCpf.getAvailableTokens() <= 0) {
            throw new MuitasTentativasException(TimeUnit.NANOSECONDS.toSeconds(
                    doCpf.estimateAbilityToConsume(1).getNanosToWaitForRefill()));
        }
    }

    /**
     * Rotas públicas do cadastro do paciente (abrir link, pedir e conferir código): toda chamada conta, por IP.
     * Mesmo limite do login, em baldes separados.
     */
    public void verificarCadastroPublico(String ip) {
        Bucket doIp = cadastroPublicoPorIp.computeIfAbsent(
                ip == null ? "desconhecido" : ip, k -> novo(propriedades.limiteLoginPorIp()));
        ConsumptionProbe consumo = doIp.tryConsumeAndReturnRemaining(1);
        if (!consumo.isConsumed()) {
            throw new MuitasTentativasException(TimeUnit.NANOSECONDS.toSeconds(consumo.getNanosToWaitForRefill()));
        }
    }

    public void registrarFalha(String cpfHash) {
        if (cpfHash != null) {
            falhasPorCpf.computeIfAbsent(cpfHash, k -> novo(propriedades.limiteFalhasPorCpf())).tryConsume(1);
        }
    }

    /** Remove os baldes já recarregados por completo (ninguém tentando por ali na última janela). */
    @Scheduled(fixedDelayString = "${clinicaviene.auth.limpeza-limite-tentativas:PT15M}")
    public void limparOciosos() {
        porIp.values().removeIf(balde -> balde.getAvailableTokens() >= propriedades.limiteLoginPorIp());
        falhasPorCpf.values().removeIf(balde -> balde.getAvailableTokens() >= propriedades.limiteFalhasPorCpf());
        cadastroPublicoPorIp.values().removeIf(balde -> balde.getAvailableTokens() >= propriedades.limiteLoginPorIp());
    }

    /** Quantos IPs/CPFs estão sendo acompanhados agora (para testes e monitoramento). */
    int emAcompanhamento() {
        return porIp.size() + falhasPorCpf.size() + cadastroPublicoPorIp.size();
    }

    private Bucket novo(int limite) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limite)
                        .refillIntervally(limite, propriedades.janelaLimiteTentativas())
                        .build())
                .withCustomTimePrecision(new RelogioDoBucket(clock))
                .build();
    }

    /** Faz o Bucket4j usar o relógio da aplicação (os testes conseguem avançar o tempo). */
    private record RelogioDoBucket(Clock clock) implements TimeMeter {

        @Override
        public long currentTimeNanos() {
            Instant agora = clock.instant();
            return TimeUnit.SECONDS.toNanos(agora.getEpochSecond()) + agora.getNano();
        }

        @Override
        public boolean isWallClockBased() {
            return true;
        }
    }
}
