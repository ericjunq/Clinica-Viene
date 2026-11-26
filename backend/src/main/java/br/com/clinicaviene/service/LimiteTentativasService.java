package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Limite de tentativas de login (Bucket4j, em memória — uma instância da aplicação):
 *
 * <ul>
 *   <li>por IP: toda tentativa conta;
 *   <li>por CPF: só as falhas contam (protege uma conta específica de força bruta vinda de vários IPs).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LimiteTentativasService {

    private final AuthProperties propriedades;
    private final Map<String, Bucket> porIp = new ConcurrentHashMap<>();
    private final Map<String, Bucket> falhasPorCpf = new ConcurrentHashMap<>();

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

    public void registrarFalha(String cpfHash) {
        if (cpfHash != null) {
            falhasPorCpf.computeIfAbsent(cpfHash, k -> novo(propriedades.limiteFalhasPorCpf())).tryConsume(1);
        }
    }

    private Bucket novo(int limite) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limite)
                        .refillIntervally(limite, propriedades.janelaLimiteTentativas())
                        .build())
                .build();
    }
}
