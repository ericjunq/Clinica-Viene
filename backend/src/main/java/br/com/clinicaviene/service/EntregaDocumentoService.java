package br.com.clinicaviene.service;

import br.com.clinicaviene.config.ConviteProperties;
import br.com.clinicaviene.config.EntregaDocumentoProperties;
import br.com.clinicaviene.auth.PacientePermissoes;
import br.com.clinicaviene.dto.DocumentoBaixadoDto;
import br.com.clinicaviene.dto.EntregaDocumentoCriadaResponse;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Documento;
import br.com.clinicaviene.entity.EntregaDocumento;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.ResponsavelLegal;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.TipoSignatario;
import br.com.clinicaviene.exceptions.CpfNaoConfereException;
import br.com.clinicaviene.exceptions.CriptografiaException;
import br.com.clinicaviene.exceptions.LinkDocumentoInvalidoException;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.AssinaturaRepository;
import br.com.clinicaviene.repository.DocumentoRepository;
import br.com.clinicaviene.repository.EntregaDocumentoRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.repository.ResponsavelLegalRepository;
import br.com.clinicaviene.util.CpfHasher;
import br.com.clinicaviene.util.CpfUtils;
import br.com.clinicaviene.util.QrCodeUtils;
import br.com.clinicaviene.util.TokenUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega de documentos ao paciente (D-094, D-026): o e-mail leva só um link de uso limitado (nunca o PDF nem dado
 * de saúde); a página pede o CPF completo antes de liberar o download. O PDF é decifrado e conferido pelo hash
 * SHA-256 registrado na assinatura.
 */
@Service
@RequiredArgsConstructor
public class EntregaDocumentoService {

    private static final String ENTIDADE = "Paciente";

    private final EntregaDocumentoRepository entregaDocumentoRepository;
    private final DocumentoRepository documentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PacienteRepository pacienteRepository;
    private final ResponsavelLegalRepository responsavelLegalRepository;
    private final ArmazenamentoService armazenamentoService;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final LimiteTentativasService limiteTentativasService;
    private final CpfHasher cpfHasher;
    private final EntregaDocumentoProperties propriedades;
    private final ConviteProperties conviteProperties;
    private final Clock clock;

    /**
     * Envia o link da ficha assinada ao e-mail do paciente — ou do responsável, quando foi ele quem assinou (ou o
     * paciente não tem e-mail).
     *
     * @return e-mail mascarado que recebeu o link, ou vazio se não houver e-mail (a recepção entrega de outra forma)
     */
    @Transactional
    public Optional<String> enviarFichaAssinada(UUID pacienteId, UUID documentoId, boolean peloResponsavel) {
        String destinatario = destinatario(pacienteId, peloResponsavel);
        if (destinatario == null) {
            return Optional.empty();
        }
        LinkGerado gerado = novoLink(documentoId);
        enviarEmail(destinatario, gerado.link());
        auditoriaService.registrar(RegistroAuditoria.sobre(null, AcaoAuditoria.DOCUMENTO_LINK_ENVIADO, ENTIDADE, pacienteId));
        return Optional.of(ConviteService.mascararEmail(destinatario));
    }

    /**
     * Novo link de download pedido pela recepção (paciente sem e-mail, link perdido ou expirado). A recepção não vê o
     * PDF: mostra o link/QR para o paciente abrir no celular dele, ou envia ao e-mail cadastrado (D-094, 2G).
     */
    @Transactional
    public EntregaDocumentoCriadaResponse gerarPelaRecepcao(
            UUID pacienteId, UUID documentoId, boolean enviarEmail, UsuarioAutenticado ator) {
        if (!PacientePermissoes.gerenciaCadastro(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para entregar documentos");
        }
        Documento documento = documentoRepository
                .findById(documentoId)
                .filter(d -> d.getPacienteId().equals(pacienteId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
        String destinatario = null;
        if (enviarEmail) {
            boolean peloResponsavel = assinaturaRepository
                    .findFirstByDocumentoId(documento.getId())
                    .map(a -> a.getTipoSignatario() == TipoSignatario.RESPONSAVEL_LEGAL)
                    .orElse(false);
            destinatario = destinatario(pacienteId, peloResponsavel);
            if (destinatario == null) {
                throw new RegraNegocioException(
                        "Paciente sem e-mail cadastrado: mostre o QR code para o paciente abrir no celular.");
            }
        }
        LinkGerado gerado = novoLink(documento.getId());
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.DOCUMENTO_LINK_GERADO, ENTIDADE, pacienteId.toString(),
                "documento " + documento.getId() + (enviarEmail ? " (enviado por e-mail)" : " (mostrado na tela)")));
        if (enviarEmail) {
            enviarEmail(destinatario, gerado.link());
            auditoriaService.registrar(
                    RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.DOCUMENTO_LINK_ENVIADO, ENTIDADE, pacienteId));
        }
        return new EntregaDocumentoCriadaResponse(
                gerado.link(), QrCodeUtils.pngDataUri(gerado.link()), gerado.expiraEm(),
                destinatario == null ? null : ConviteService.mascararEmail(destinatario));
    }

    private record LinkGerado(String link, Instant expiraEm) {}

    private LinkGerado novoLink(UUID documentoId) {
        Instant agora = agora();
        Instant expiraEm = agora.plus(propriedades.validade());
        String token = TokenUtils.gerar();
        entregaDocumentoRepository.save(new EntregaDocumento(documentoId, TokenUtils.sha256(token), agora, expiraEm));
        return new LinkGerado(conviteProperties.urlPublica().replaceAll("/+$", "") + "/documento#" + token, expiraEm);
    }

    /** E-mail do responsável quando foi ele quem assinou (ou o paciente não tem e-mail); senão, o do paciente. */
    private String destinatario(UUID pacienteId, boolean peloResponsavel) {
        Paciente paciente = pacienteRepository.findById(pacienteId).orElseThrow(PacienteNaoEncontradoException::new);
        String emailResponsavel = responsavelLegalRepository.findByPacienteId(pacienteId)
                .map(ResponsavelLegal::getEmail)
                .orElse(null);
        return peloResponsavel && emailResponsavel != null
                ? emailResponsavel
                : Optional.ofNullable(paciente.getEmail()).orElse(emailResponsavel);
    }

    private void enviarEmail(String destinatario, String link) {
        emailService.enviar(
                destinatario,
                "Viene Odontologia & Saúde — sua ficha assinada",
                "Olá!\n\nSeu cadastro na Viene Odontologia & Saúde foi concluído. Para baixar a ficha assinada, acesse:\n\n"
                        + link + "\n\n"
                        + "Por segurança, a página vai pedir o CPF do paciente (ou do responsável, se o paciente não "
                        + "tiver CPF). O link vale por " + propriedades.validade().toDays() + " dias.\n"
                        + "Se você não reconhece este cadastro, ignore esta mensagem e avise a clínica.\n");
    }

    /** Libera o PDF se o link estiver válido e o CPF conferir; CPF errado conta tentativa (e pode bloquear o link). */
    @Transactional(noRollbackFor = CpfNaoConfereException.class)
    public DocumentoBaixadoDto baixar(String token, String cpf, String ip) {
        limiteTentativasService.verificarCadastroPublico(ip);
        if (token == null || token.isBlank()) {
            throw new LinkDocumentoInvalidoException();
        }
        Instant agora = agora();
        EntregaDocumento entrega = entregaDocumentoRepository
                .findByTokenHash(TokenUtils.sha256(token))
                .filter(e -> e.utilizavel(agora, propriedades.maximoDownloads(), propriedades.tentativasCpf()))
                .orElseThrow(LinkDocumentoInvalidoException::new);
        Documento documento = documentoRepository.findById(entrega.getDocumentoId()).orElseThrow();
        if (!cpfConfere(documento.getPacienteId(), cpf)) {
            entrega.registrarCpfIncorreto();
            auditoriaService.registrar(
                    RegistroAuditoria.sobre(null, AcaoAuditoria.DOCUMENTO_CPF_INCORRETO, ENTIDADE, documento.getPacienteId()));
            throw new CpfNaoConfereException();
        }
        byte[] pdf = armazenamentoService.ler(documento.getChaveArmazenamento());
        if (!iguais(FichaAnamneseService.sha256(pdf), documento.getSha256())) {
            throw new CriptografiaException("O documento guardado não confere com o hash registrado na assinatura.");
        }
        entrega.registrarDownload(agora);
        auditoriaService.registrar(new RegistroAuditoria(
                null, AcaoAuditoria.DOCUMENTO_BAIXADO, ENTIDADE, documento.getPacienteId().toString(),
                "documento " + documento.getId() + " baixado pelo link"));
        return new DocumentoBaixadoDto("ficha-cadastro-viene.pdf", pdf);
    }

    /** CPF do paciente; se o paciente não tiver CPF (menor), o do responsável legal. */
    private boolean cpfConfere(UUID pacienteId, String cpf) {
        String normalizado = CpfUtils.normalizar(cpf);
        if (normalizado == null || !CpfUtils.valido(normalizado)) {
            return false;
        }
        String hash = cpfHasher.calcular(normalizado);
        Paciente paciente = pacienteRepository.findById(pacienteId).orElseThrow();
        if (paciente.getCpfHash() != null) {
            return iguais(hash, paciente.getCpfHash());
        }
        return responsavelLegalRepository
                .findByPacienteId(pacienteId)
                .map(r -> iguais(hash, r.getCpfHash()))
                .orElse(false);
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
