package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AgendaPermissoes;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Consultorio;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.ConsultorioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultórios (CONTEXTO §7): o MASTER cadastra; quem usa a agenda lista. */
@Service
@RequiredArgsConstructor
public class ConsultorioService {

    private static final String ENTIDADE = "Consultorio";

    private final ConsultorioRepository consultorioRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Consultorio> listar(UsuarioAutenticado ator) {
        if (!AgendaPermissoes.usaAgenda(ator.perfil())) {
            throw new AccessDeniedException("Sem acesso à agenda");
        }
        return consultorioRepository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Consultorio criar(String nome, UsuarioAutenticado ator) {
        exigirConfiguracao(ator);
        String limpo = nome.trim();
        if (consultorioRepository.existsByNomeIgnoreCase(limpo)) {
            throw new RegraNegocioException("Já existe um consultório com este nome.");
        }
        Consultorio c = consultorioRepository.save(new Consultorio(limpo, agora()));
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.CONSULTORIO_CRIADO, ENTIDADE, c.getId()));
        return c;
    }

    @Transactional
    public Consultorio atualizar(UUID id, String nome, boolean ativo, UsuarioAutenticado ator) {
        exigirConfiguracao(ator);
        Consultorio c = consultorioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consultório não encontrado."));
        String limpo = nome.trim();
        if (consultorioRepository.existsByNomeIgnoreCaseAndIdNot(limpo, id)) {
            throw new RegraNegocioException("Já existe um consultório com este nome.");
        }
        c.atualizar(limpo, ativo, agora());
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.CONSULTORIO_ALTERADO, ENTIDADE, id));
        return c;
    }

    /** Consultório que pode receber horários e agendamentos. */
    @Transactional // sem readOnly: também é chamado no meio de gravações
    public Consultorio ativo(UUID id) {
        return consultorioRepository.findById(id)
                .filter(Consultorio::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Consultório não encontrado ou desativado."));
    }

    private static void exigirConfiguracao(UsuarioAutenticado ator) {
        if (!AgendaPermissoes.configuraAgenda(ator.perfil())) {
            throw new AccessDeniedException("Só o MASTER configura consultórios");
        }
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
