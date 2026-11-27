package br.com.clinicaviene.service;

import br.com.clinicaviene.config.CadastroPublicoProperties;
import br.com.clinicaviene.config.EnvioCodigoProperties;
import br.com.clinicaviene.dto.CodigoCadastroEnviadoResponse;
import br.com.clinicaviene.dto.ConviteAbertoResponse;
import br.com.clinicaviene.dto.DadosCadastroDto;
import br.com.clinicaviene.dto.DecisaoTermoRequest;
import br.com.clinicaviene.dto.FichaCadastroRequest;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.RevisaoCadastroResponse;
import br.com.clinicaviene.dto.SessaoCadastroResponse;
import br.com.clinicaviene.entity.ConviteCadastro;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.SessaoCadastro;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.SituacaoConvite;
import br.com.clinicaviene.exceptions.CodigoInvalidoException;
import br.com.clinicaviene.exceptions.ConviteInvalidoException;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.exceptions.SessaoCadastroInvalidaException;
import br.com.clinicaviene.repository.ConviteCadastroRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.repository.SessaoCadastroRepository;
import br.com.clinicaviene.util.RegraReenvio;
import br.com.clinicaviene.util.TokenUtils;
import br.com.clinicaviene.validation.RespostasAnamneseValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fluxo público do cadastro (CONTEXTO §4.2, D-091, D-100): o paciente abre o convite, confirma a identidade
 * (código no e-mail no modo LINK; no quiosque a recepção já conferiu o documento), recebe uma sessão curta de
 * preenchimento, confere os dados, responde a anamnese e decide sobre os termos. Aqui a ficha é só revisada:
 * a gravação acontece com a assinatura (2F) — nada da anamnese fica no servidor antes disso.
 */
@Service
@RequiredArgsConstructor
public class CadastroPublicoService {

    private static final String ENTIDADE = "Paciente";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ConviteCadastroRepository conviteCadastroRepository;
    private final SessaoCadastroRepository sessaoCadastroRepository;
    private final PacienteRepository pacienteRepository;
    private final ConviteService conviteService;
    private final PacienteService pacienteService;
    private final ModeloAnamneseService modeloAnamneseService;
    private final TermoService termoService;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final LimiteTentativasService limiteTentativasService;
    private final CadastroPublicoProperties propriedades;
    private final EnvioCodigoProperties regrasCodigo;
    private final Clock clock;

    /** Primeira tela do link: só o primeiro nome e o que vem a seguir (código ou quiosque). */
    @Transactional
    public ConviteAbertoResponse abrir(String token, String ip) {
        limiteTentativasService.verificarCadastroPublico(ip);
        ConviteCadastro convite = conviteService.ativoPorToken(token);
        Paciente paciente = paciente(convite);
        boolean link = convite.getModo() == ModoConvite.LINK;
        return new ConviteAbertoResponse(
                convite.getModo(),
                primeiroNome(paciente),
                link,
                link ? ConviteService.mascararEmail(conviteService.destinatario(paciente)) : null);
    }

    /** LINK: envia um código de 6 dígitos ao e-mail do convite, com as regras de reenvio da D-073. */
    @Transactional
    public CodigoCadastroEnviadoResponse enviarCodigo(String token, String ip, String dispositivo) {
        limiteTentativasService.verificarCadastroPublico(ip);
        ConviteCadastro convite = conviteService.ativoPorToken(token);
        exigirLink(convite);
        Instant agora = agora();
        List<SessaoCadastro> recentes = recentes(convite, agora);
        if (!recentes.isEmpty()) {
            Instant liberado = RegraReenvio.liberadoEm(recentes.size(), recentes.getLast().getCodigoEnviadoEm(), regrasCodigo);
            if (agora.isBefore(liberado)) {
                throw new MuitasTentativasException(Duration.between(agora, liberado).toSeconds());
            }
        }
        Paciente paciente = paciente(convite);
        String email = conviteService.destinatario(paciente);
        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        Instant expiraEm = agora.plus(regrasCodigo.validade());
        sessaoCadastroRepository.save(
                SessaoCadastro.pendente(convite.getId(), TokenUtils.sha256(codigo), expiraEm, ip, dispositivo, agora));
        emailService.enviar(
                email,
                "Viene Odontologia & Saúde — código para seu cadastro",
                "Seu código para continuar o cadastro é: " + codigo + "\n\n"
                        + "Ele vale por " + regrasCodigo.validade().toMinutes() + " minutos. Não compartilhe este código.\n"
                        + "Se você não pediu, ignore esta mensagem e avise a clínica.\n");
        auditoriaService.registrar(
                RegistroAuditoria.sobre(null, AcaoAuditoria.CADASTRO_CODIGO_ENVIADO, ENTIDADE, convite.getPacienteId()));
        return new CodigoCadastroEnviadoResponse(
                ConviteService.mascararEmail(email), RegraReenvio.liberadoEm(recentes.size() + 1, agora, regrasCodigo),
                expiraEm);
    }

    /**
     * LINK: confere o código mais recente; certo, abre a sessão de preenchimento. Errar conta tentativa (o código
     * é invalidado depois do limite) — por isso a transação não volta atrás no erro.
     */
    @Transactional(noRollbackFor = CodigoInvalidoException.class)
    public SessaoCadastroResponse confirmarCodigo(String token, String codigo, String ip, String dispositivo) {
        limiteTentativasService.verificarCadastroPublico(ip);
        ConviteCadastro convite = conviteService.ativoPorToken(token);
        exigirLink(convite);
        Instant agora = agora();
        List<SessaoCadastro> recentes = recentes(convite, agora);
        SessaoCadastro atual = recentes.isEmpty() ? null : recentes.getLast();
        if (atual == null || !atual.codigoUtilizavel(agora, regrasCodigo.tentativas())) {
            throw new CodigoInvalidoException();
        }
        if (codigo == null || !iguais(TokenUtils.sha256(codigo.trim()), atual.getCodigoHash())) {
            atual.registrarFalhaCodigo();
            auditoriaService.registrar(
                    RegistroAuditoria.sobre(null, AcaoAuditoria.CADASTRO_CODIGO_INCORRETO, ENTIDADE, convite.getPacienteId()));
            throw new CodigoInvalidoException();
        }
        String sessao = TokenUtils.gerar();
        Instant expiraEm = agora.plus(propriedades.validadeSessao());
        atual.confirmar(TokenUtils.sha256(sessao), expiraEm, agora);
        auditoriaService.registrar(RegistroAuditoria.sobre(
                null, AcaoAuditoria.CADASTRO_IDENTIDADE_CONFIRMADA, ENTIDADE, convite.getPacienteId()));
        return new SessaoCadastroResponse(sessao, expiraEm);
    }

    /** QUIOSQUE: a recepção já conferiu o documento ao gerar o convite; a sessão abre direto. */
    @Transactional
    public SessaoCadastroResponse iniciarQuiosque(String token, String ip, String dispositivo) {
        limiteTentativasService.verificarCadastroPublico(ip);
        ConviteCadastro convite = conviteService.ativoPorToken(token);
        if (convite.getModo() != ModoConvite.QUIOSQUE) {
            throw new RegraNegocioException("Este link é para o celular do paciente: confirme com o código do e-mail.");
        }
        Instant agora = agora();
        String sessao = TokenUtils.gerar();
        Instant expiraEm = agora.plus(propriedades.validadeSessao());
        sessaoCadastroRepository.save(SessaoCadastro.confirmadaNoQuiosque(
                convite.getId(), TokenUtils.sha256(sessao), expiraEm, ip, dispositivo, agora));
        auditoriaService.registrar(
                RegistroAuditoria.sobre(null, AcaoAuditoria.CADASTRO_QUIOSQUE_INICIADO, ENTIDADE, convite.getPacienteId()));
        return new SessaoCadastroResponse(sessao, expiraEm);
    }

    /** Pré-cadastro para conferir, modelo de anamnese vigente e termos vigentes. */
    @Transactional
    public FormularioCadastroResponse formulario(String token, String tokenSessao) {
        ConviteCadastro convite = conviteDaSessao(token, tokenSessao);
        DadosCadastroDto dados = pacienteService.dadosParaConferencia(convite.getPacienteId());
        ModeloAnamnese modelo = modeloVigente(convite);
        List<FormularioCadastroResponse.Termo> termos = termosVigentes().stream()
                .map(t -> new FormularioCadastroResponse.Termo(
                        t.getId(), t.getTipo(), t.getTipo().obrigatorio(), t.getVersao(), t.getTitulo(), t.getTexto()))
                .toList();
        auditoriaService.registrar(
                RegistroAuditoria.sobre(null, AcaoAuditoria.CADASTRO_FORMULARIO_LIDO, ENTIDADE, convite.getPacienteId()));
        return new FormularioCadastroResponse(
                convite.getModo(),
                menor(dados),
                dados,
                new FormularioCadastroResponse.Anamnese(
                        modelo.getId(), modelo.getTitulo(), modelo.getVersao(), modeloAnamneseService.perguntas(modelo)),
                termos);
    }

    /**
     * Confere a ficha inteira antes da assinatura: dados (regras do cadastro), anamnese contra o modelo vigente e
     * uma decisão para cada termo vigente, com a política de privacidade aceita.
     */
    @Transactional
    public RevisaoCadastroResponse revisar(String token, String tokenSessao, FichaCadastroRequest ficha) {
        ConviteCadastro convite = conviteDaSessao(token, tokenSessao);
        DadosCadastroDto dados = pacienteService.validarFicha(convite.getPacienteId(), ficha.dados());

        ModeloAnamnese modelo = modeloVigente(convite);
        if (!modelo.getId().equals(ficha.modeloAnamneseId())) {
            throw new RegraNegocioException("A ficha de saúde foi atualizada pela clínica. Recarregue a página para continuar.");
        }
        RespostasAnamneseValidator.validar(modeloAnamneseService.perguntas(modelo), ficha.respostas());

        validarTermos(ficha.termos());
        auditoriaService.registrar(
                RegistroAuditoria.sobre(null, AcaoAuditoria.CADASTRO_REVISADO, ENTIDADE, convite.getPacienteId()));
        return new RevisaoCadastroResponse(true, menor(dados));
    }

    private void validarTermos(List<DecisaoTermoRequest> decisoes) {
        List<TermoVersao> vigentes = termosVigentes();
        Set<UUID> esperados = vigentes.stream().map(TermoVersao::getId).collect(Collectors.toSet());
        Set<UUID> recebidos = new HashSet<>();
        for (DecisaoTermoRequest d : decisoes) {
            if (!recebidos.add(d.termoVersaoId())) {
                throw new RegraNegocioException("Termo repetido na ficha.");
            }
        }
        if (!recebidos.equals(esperados)) {
            throw new RegraNegocioException("Os termos foram atualizados pela clínica. Recarregue a página para continuar.");
        }
        Set<UUID> aceitos = decisoes.stream()
                .filter(DecisaoTermoRequest::aceitou)
                .map(DecisaoTermoRequest::termoVersaoId)
                .collect(Collectors.toSet());
        for (TermoVersao termo : vigentes) {
            if (termo.getTipo().obrigatorio() && !aceitos.contains(termo.getId())) {
                throw new RegraNegocioException("É preciso ler e aceitar a política de privacidade para concluir o cadastro.");
            }
        }
    }

    /** Sessão confirmada, dentro do prazo, do mesmo convite, e convite ainda não usado nem cancelado. */
    private ConviteCadastro conviteDaSessao(String token, String tokenSessao) {
        if (token == null || token.isBlank() || tokenSessao == null || tokenSessao.isBlank()) {
            throw new SessaoCadastroInvalidaException();
        }
        Instant agora = agora();
        SessaoCadastro sessao = sessaoCadastroRepository
                .findByTokenHash(TokenUtils.sha256(tokenSessao))
                .filter(s -> s.valida(agora))
                .orElseThrow(SessaoCadastroInvalidaException::new);
        return conviteCadastroRepository
                .findByTokenHash(TokenUtils.sha256(token))
                .filter(c -> c.getId().equals(sessao.getConviteId()))
                .filter(c -> c.situacao(agora) != SituacaoConvite.USADO && c.situacao(agora) != SituacaoConvite.CANCELADO)
                .orElseThrow(SessaoCadastroInvalidaException::new);
    }

    private ModeloAnamnese modeloVigente(ConviteCadastro convite) {
        return modeloAnamneseService
                .vigente(convite.getAreaAtuacao())
                .orElseThrow(() -> new RegraNegocioException(
                        "A clínica ainda não publicou a ficha de saúde. Avise a recepção."));
    }

    private List<TermoVersao> termosVigentes() {
        List<TermoVersao> vigentes = termoService.vigentes();
        if (vigentes.stream().noneMatch(t -> t.getTipo().obrigatorio())) {
            throw new RegraNegocioException("A clínica ainda não publicou a política de privacidade. Avise a recepção.");
        }
        return vigentes;
    }

    private List<SessaoCadastro> recentes(ConviteCadastro convite, Instant agora) {
        return sessaoCadastroRepository.findByConviteIdAndCodigoEnviadoEmAfterOrderByCodigoEnviadoEmAsc(
                convite.getId(), agora.minus(regrasCodigo.bloqueio()));
    }

    private Paciente paciente(ConviteCadastro convite) {
        return pacienteRepository.findById(convite.getPacienteId()).orElseThrow(PacienteNaoEncontradoException::new);
    }

    private boolean menor(DadosCadastroDto dados) {
        return dados.dataNascimento().plusYears(Paciente.MAIORIDADE).isAfter(pacienteService.hoje());
    }

    private static void exigirLink(ConviteCadastro convite) {
        if (convite.getModo() != ModoConvite.LINK) {
            throw new ConviteInvalidoException();
        }
    }

    private static String primeiroNome(Paciente paciente) {
        return paciente.getNomeCompleto().split(" ")[0];
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
