package br.com.clinicaviene.service;

import br.com.clinicaviene.config.EnvioCodigoProperties;
import br.com.clinicaviene.entity.EnvioCodigo;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.FinalidadeEnvio;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.repository.EnvioCodigoRepository;
import br.com.clinicaviene.util.RegraReenvio;
import br.com.clinicaviene.util.TokenUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envio de códigos de 6 dígitos por e-mail com as regras de reenvio da D-073:
 *
 * <ul>
 *   <li>espera crescente entre envios (padrão 1, 2, 5 e 10 min);
 *   <li>no máximo N envios (padrão 5) por usuário e finalidade; depois, só após o bloqueio (padrão 1 h);
 *   <li>um envio novo invalida o código anterior; o código expira (padrão 10 min) e morre após N erros.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EnvioCodigoService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final EnvioCodigoRepository envioCodigoRepository;
    private final EmailService emailService;
    private final EnvioCodigoProperties envioCodigoProperties;
    private final Clock clock;

    /**
     * Gera e envia um código novo, se as regras de reenvio permitirem.
     *
     * @return a partir de quando um novo envio será permitido (para o frontend mostrar a contagem)
     * @throws MuitasTentativasException se ainda estiver na espera ou no bloqueio (sem marcar a transação do
     *     chamador para rollback: quem chama pode tratar e seguir)
     */
    @Transactional(noRollbackFor = MuitasTentativasException.class)
    public Instant enviar(Usuario usuario, FinalidadeEnvio finalidade) {
        Instant agora = agora();
        List<EnvioCodigo> recentes = recentes(usuario, finalidade, agora);
        if (!recentes.isEmpty()) {
            Instant liberadoEm = liberadoEm(recentes.size(), recentes.getLast().getEnviadoEm());
            if (agora.isBefore(liberadoEm)) {
                throw new MuitasTentativasException(Duration.between(agora, liberadoEm).toSeconds());
            }
        }
        recentes.forEach(anterior -> anterior.invalidar(agora));

        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        envioCodigoRepository.save(new EnvioCodigo(
                usuario.getId(), finalidade, TokenUtils.sha256(codigo), agora, agora.plus(envioCodigoProperties.validade())));
        emailService.enviar(usuario.getEmail(), assunto(finalidade), texto(finalidade, codigo));
        return liberadoEm(recentes.size() + 1, agora);
    }

    /**
     * Confere o código mais recente da finalidade. Errar conta uma tentativa (e pode invalidar o código).
     * Chamadores devem usar {@code noRollbackFor} para a contagem de erros não se perder.
     */
    @Transactional
    public boolean verificar(Usuario usuario, FinalidadeEnvio finalidade, String codigo) {
        Instant agora = agora();
        List<EnvioCodigo> recentes = recentes(usuario, finalidade, agora);
        if (recentes.isEmpty()) {
            return false;
        }
        EnvioCodigo atual = recentes.getLast();
        if (!atual.utilizavel(agora)) {
            return false;
        }
        if (codigo != null && iguais(TokenUtils.sha256(codigo.trim()), atual.getCodigoHash())) {
            atual.usar(agora);
            return true;
        }
        atual.registrarFalha(envioCodigoProperties.tentativas(), agora);
        return false;
    }

    /** Envios da janela de bloqueio, do mais antigo ao mais novo. */
    private List<EnvioCodigo> recentes(Usuario usuario, FinalidadeEnvio finalidade, Instant agora) {
        return envioCodigoRepository.findAllByUsuarioIdAndFinalidadeAndEnviadoEmAfterOrderByEnviadoEmAsc(
                usuario.getId(), finalidade, agora.minus(envioCodigoProperties.bloqueio()));
    }

    Instant liberadoEm(int enviosNaJanela, Instant ultimoEnvio) {
        return RegraReenvio.liberadoEm(enviosNaJanela, ultimoEnvio, envioCodigoProperties);
    }

    private static String assunto(FinalidadeEnvio finalidade) {
        return switch (finalidade) {
            case DOIS_FATORES_EMAIL -> "ClinicaViene — código de acesso";
            case RECUPERACAO_SENHA -> "ClinicaViene — redefinição de senha";
        };
    }

    private String texto(FinalidadeEnvio finalidade, String codigo) {
        String uso = switch (finalidade) {
            case DOIS_FATORES_EMAIL -> "para concluir o seu acesso ao sistema";
            case RECUPERACAO_SENHA -> "para redefinir a sua senha";
        };
        return "Seu código " + uso + " é: " + codigo + "\n\n"
                + "Ele vale por " + envioCodigoProperties.validade().toMinutes() + " minutos e só pode ser usado uma vez.\n"
                + "Se não foi você quem pediu, ignore este e-mail e avise a clínica.\n";
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
