package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.PacientePermissoes;
import br.com.clinicaviene.config.ConviteProperties;
import br.com.clinicaviene.config.EnvioCodigoProperties;
import br.com.clinicaviene.dto.ConviteCriadoResponse;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ConviteCadastro;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.ResponsavelLegal;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.SituacaoConvite;
import br.com.clinicaviene.exceptions.ConviteInvalidoException;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.ConviteCadastroRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.repository.ResponsavelLegalRepository;
import br.com.clinicaviene.util.QrCodeUtils;
import br.com.clinicaviene.util.RegraReenvio;
import br.com.clinicaviene.util.TokenUtils;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convites de cadastro (CONTEXTO §4.2, D-091). Só um convite ativo por paciente: gerar outro cancela o
 * anterior. O token (≥ 256 bits) só aparece na criação; no banco fica o hash. Envio por e-mail segue a D-073.
 */
@Service
@RequiredArgsConstructor
public class ConviteService {

    private static final String ENTIDADE = "Paciente";

    private final ConviteCadastroRepository conviteCadastroRepository;
    private final PacienteRepository pacienteRepository;
    private final ResponsavelLegalRepository responsavelLegalRepository;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final ConviteProperties conviteProperties;
    private final EnvioCodigoProperties regrasReenvio;
    private final Clock clock;

    @Transactional
    public ConviteCriadoResponse criar(
            UUID pacienteId, ModoConvite modo, boolean enviarEmail, boolean documentoConferido, UsuarioAutenticado ator) {
        exigirRecepcao(ator);
        Paciente paciente = pacienteRepository.findById(pacienteId).orElseThrow(PacienteNaoEncontradoException::new);
        Instant agora = agora();

        if (modo == ModoConvite.QUIOSQUE && !documentoConferido) {
            throw new RegraNegocioException("No quiosque, confira o documento com foto do paciente antes de abrir o cadastro.");
        }
        if (modo == ModoConvite.QUIOSQUE && enviarEmail) {
            throw new RegraNegocioException("O convite de quiosque é aberto neste aparelho; não é enviado por e-mail.");
        }
        // LINK sempre precisa de e-mail: é por ele que o paciente confirma a identidade ao abrir (D-100).
        String destinatario = modo == ModoConvite.LINK ? destinatario(paciente) : null;
        Instant podeReenviarEm = null;
        if (enviarEmail) {
            List<ConviteCadastro> recentes = conviteCadastroRepository
                    .findByPacienteIdAndEnviadoPorEmailEmAfterOrderByEnviadoPorEmailEmAsc(pacienteId, agora.minus(regrasReenvio.bloqueio()));
            if (!recentes.isEmpty()) {
                Instant liberado = RegraReenvio.liberadoEm(recentes.size(), recentes.getLast().getEnviadoPorEmailEm(), regrasReenvio);
                if (agora.isBefore(liberado)) {
                    throw new MuitasTentativasException(Duration.between(agora, liberado).toSeconds());
                }
            }
            podeReenviarEm = RegraReenvio.liberadoEm(recentes.size() + 1, agora, regrasReenvio);
        }

        conviteCadastroRepository.findByPacienteIdOrderByCriadoEmDesc(pacienteId).forEach(anterior -> {
            if (anterior.situacao(agora) == SituacaoConvite.ATIVO) {
                anterior.cancelar(agora);
            }
        });

        String token = TokenUtils.gerar();
        Instant expiraEm = agora.plus(modo == ModoConvite.LINK ? conviteProperties.validadeLink() : conviteProperties.validadeQuiosque());
        ConviteCadastro convite = conviteCadastroRepository.save(new ConviteCadastro(
                pacienteId, TokenUtils.sha256(token), modo, ator.id(), modo == ModoConvite.QUIOSQUE ? ator.id() : null, agora,
                expiraEm));
        String link = conviteProperties.urlPublica().replaceAll("/+$", "") + "/cadastro/" + token;
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.CONVITE_CRIADO, ENTIDADE, pacienteId.toString(), "modo " + modo));

        if (enviarEmail) {
            convite.marcarEnviadoPorEmail(agora);
            emailService.enviar(destinatario, "Viene Odontologia & Saúde — complete seu cadastro", textoEmail(paciente, link, Duration.between(agora, expiraEm)));
            auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.CONVITE_ENVIADO_EMAIL, ENTIDADE, pacienteId));
        }
        return new ConviteCriadoResponse(
                convite.getId(), modo, link, QrCodeUtils.pngDataUri(link), expiraEm,
                enviarEmail ? mascararEmail(destinatario) : null, podeReenviarEm);
    }

    @Transactional(readOnly = true)
    public List<ConviteCadastro> listar(UUID pacienteId, UsuarioAutenticado ator) {
        exigirRecepcao(ator);
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new PacienteNaoEncontradoException();
        }
        return conviteCadastroRepository.findByPacienteIdOrderByCriadoEmDesc(pacienteId);
    }

    @Transactional
    public void cancelar(UUID pacienteId, UUID conviteId, UsuarioAutenticado ator) {
        exigirRecepcao(ator);
        ConviteCadastro convite = conviteCadastroRepository
                .findById(conviteId)
                .filter(c -> c.getPacienteId().equals(pacienteId))
                .orElseThrow(ConviteInvalidoException::new);
        convite.cancelar(agora());
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.CONVITE_CANCELADO, ENTIDADE, pacienteId));
    }

    /**
     * Convite ativo pelo token (fluxo público — 2E). Inválido, expirado, cancelado ou usado: mesma resposta.
     * Não é somente leitura: a busca trava a linha (evita uso duplo do mesmo convite em requisições simultâneas).
     */
    @Transactional
    public ConviteCadastro ativoPorToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ConviteInvalidoException();
        }
        return conviteCadastroRepository
                .findByTokenHash(TokenUtils.sha256(token))
                .filter(c -> c.situacao(agora()) == SituacaoConvite.ATIVO)
                .orElseThrow(ConviteInvalidoException::new);
    }

    public Instant agoraClinica() {
        return agora();
    }

    /** E-mail do paciente; se menor sem e-mail próprio, o do responsável (que é quem preenche e assina). */
    public String destinatario(Paciente paciente) {
        String email = paciente.getEmail();
        if (email == null) {
            email = responsavelLegalRepository.findByPacienteId(paciente.getId()).map(ResponsavelLegal::getEmail).orElse(null);
        }
        if (email == null) {
            throw new RegraNegocioException(
                    "Paciente sem e-mail cadastrado: o link exige confirmação por código no e-mail. Use o quiosque.");
        }
        return email;
    }

    private static String textoEmail(Paciente paciente, String link, Duration validade) {
        String primeiroNome = paciente.getNomeCompleto().split(" ")[0];
        return "Olá, " + primeiroNome + "!\n\n"
                + "Para agilizar seu atendimento na Viene Odontologia & Saúde, complete seu cadastro e sua ficha de saúde "
                + "pelo link abaixo:\n\n" + link + "\n\n"
                + "O link é pessoal, só pode ser usado uma vez e vale por "
                + validade.toHours() + " horas.\n"
                + "Se você não esperava esta mensagem, ignore-a e avise a clínica.\n";
    }

    public static String mascararEmail(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 1) {
            return "***" + email.substring(Math.max(arroba, 0));
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    private static void exigirRecepcao(UsuarioAutenticado ator) {
        if (!PacientePermissoes.gerenciaCadastro(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para gerar convites");
        }
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
