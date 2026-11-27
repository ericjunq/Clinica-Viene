package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.PacientePermissoes;
import br.com.clinicaviene.dto.ConsentimentoResponse;
import br.com.clinicaviene.dto.DecisaoConsentimentoDto;
import br.com.clinicaviene.dto.EvidenciaAceiteDto;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.AceiteTermo;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.DecisaoTermo;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.AceiteTermoRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.repository.TermoVersaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decisões do paciente sobre os termos (D-093). Cada decisão vira um registro (somente inclusão) com as
 * evidências; o estado atual é a decisão mais recente. A política de privacidade é ciência obrigatória e não
 * se revoga; os consentimentos opcionais são desmarcados por padrão e revogáveis a qualquer momento.
 */
@Service
@RequiredArgsConstructor
public class ConsentimentoService {

    private static final String ENTIDADE = "Paciente";

    private final AceiteTermoRepository aceiteTermoRepository;
    private final TermoVersaoRepository termoVersaoRepository;
    private final PacienteRepository pacienteRepository;
    private final TermoService termoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    /**
     * Registra as decisões do paciente no fechamento do cadastro (fluxo público — 2E). Exige a ciência da
     * política de privacidade; cada consentimento opcional vigente fica ACEITO (marcado) ou RECUSADO
     * (desmarcado — prova de que a caixa foi oferecida desmarcada).
     */
    @Transactional
    public void registrarDecisoes(UUID pacienteId, List<DecisaoConsentimentoDto> decisoes, EvidenciaAceiteDto evidencia) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new PacienteNaoEncontradoException();
        }
        Map<TipoTermo, Boolean> marcadas = new EnumMap<>(TipoTermo.class);
        for (DecisaoConsentimentoDto d : decisoes) {
            marcadas.put(d.tipo(), d.aceito());
        }
        if (!Boolean.TRUE.equals(marcadas.get(TipoTermo.POLITICA_PRIVACIDADE))) {
            throw new RegraNegocioException("É preciso ler e aceitar a política de privacidade para concluir o cadastro.");
        }
        if (termoService.vigente(TipoTermo.POLITICA_PRIVACIDADE).isEmpty()) {
            throw TermoService.semVigente(TipoTermo.POLITICA_PRIVACIDADE);
        }
        Instant agora = agora();
        for (TermoVersao termo : termoService.vigentes()) {
            boolean aceito = Boolean.TRUE.equals(marcadas.get(termo.getTipo()));
            aceiteTermoRepository.save(new AceiteTermo(
                    termo, pacienteId, aceito ? DecisaoTermo.ACEITO : DecisaoTermo.RECUSADO, evidencia.origem(),
                    evidencia.peloResponsavel(), evidencia.ip(), evidencia.dispositivo(), evidencia.registradoPor(),
                    null, agora));
        }
        auditoriaService.registrar(RegistroAuditoria.sobre(
                evidencia.registradoPor(), AcaoAuditoria.CONSENTIMENTO_REGISTRADO, ENTIDADE, pacienteId));
    }

    /** Revogação pedida pelo paciente na recepção (ADMIN/MASTER registram). */
    @Transactional
    public void revogar(UUID pacienteId, TipoTermo tipo, String justificativa, String ip, UsuarioAutenticado ator) {
        exigirRecepcao(ator);
        if (tipo.obrigatorio()) {
            throw new RegraNegocioException(
                    "A política de privacidade é ciência, não consentimento: não se revoga. O atendimento e o "
                            + "prontuário seguem a base legal de saúde e obrigação legal.");
        }
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new PacienteNaoEncontradoException();
        }
        AceiteTermo atual = aceiteTermoRepository
                .findFirstByPacienteIdAndTipoOrderByRegistradoEmDesc(pacienteId, tipo)
                .filter(a -> a.getDecisao() == DecisaoTermo.ACEITO)
                .orElseThrow(() -> new RegraNegocioException("Não há consentimento ativo para revogar."));
        TermoVersao termo = termoVersaoRepository.findById(atual.getTermoVersaoId()).orElseThrow();
        String motivo = justificativa == null || justificativa.isBlank() ? null : justificativa.trim();
        aceiteTermoRepository.save(new AceiteTermo(
                termo, pacienteId, DecisaoTermo.REVOGADO, OrigemAlteracao.RECEPCAO, false, ip, null, ator.id(), motivo,
                agora()));
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.CONSENTIMENTO_REVOGADO, ENTIDADE, pacienteId.toString(), motivo));
    }

    /** Situação de cada termo para o paciente (recepção/MASTER). */
    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> situacao(UUID pacienteId, UsuarioAutenticado ator) {
        exigirRecepcao(ator);
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new PacienteNaoEncontradoException();
        }
        List<ConsentimentoResponse> lista = new ArrayList<>();
        for (TipoTermo tipo : TipoTermo.values()) {
            Optional<AceiteTermo> ultimo = aceiteTermoRepository.findFirstByPacienteIdAndTipoOrderByRegistradoEmDesc(pacienteId, tipo);
            lista.add(ultimo.map(a -> new ConsentimentoResponse(
                            tipo, tipo.obrigatorio(), a.getDecisao(),
                            termoVersaoRepository.findById(a.getTermoVersaoId()).map(TermoVersao::getVersao).orElse(null),
                            a.getRegistradoEm(), a.getOrigem(), a.isPeloResponsavel()))
                    .orElse(new ConsentimentoResponse(tipo, tipo.obrigatorio(), null, null, null, null, false)));
        }
        return lista;
    }

    /**
     * O paciente consentiu (e não revogou) este uso? Usado antes de qualquer envio/uso opcional (lembretes,
     * promoções, imagem, pesquisa): sem consentimento ativo, não se usa.
     */
    @Transactional(readOnly = true)
    public boolean temConsentimentoAtivo(UUID pacienteId, TipoTermo tipo) {
        return aceiteTermoRepository
                .findFirstByPacienteIdAndTipoOrderByRegistradoEmDesc(pacienteId, tipo)
                .map(a -> a.getDecisao() == DecisaoTermo.ACEITO)
                .orElse(false);
    }

    private static void exigirRecepcao(UsuarioAutenticado ator) {
        if (!PacientePermissoes.gerenciaCadastro(ator.perfil())) {
            throw new AccessDeniedException("Sem permissão para os consentimentos do paciente");
        }
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
