package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AgendaPermissoes;
import br.com.clinicaviene.config.AgendaProperties;
import br.com.clinicaviene.dto.ExcecaoAgendaResponse;
import br.com.clinicaviene.dto.HorarioFixoResponse;
import br.com.clinicaviene.dto.HorariosProfissionalResponse;
import br.com.clinicaviene.dto.IntervaloDisponivelDto;
import br.com.clinicaviene.dto.NovaExcecaoAgendaRequest;
import br.com.clinicaviene.dto.NovoHorarioFixoRequest;
import br.com.clinicaviene.dto.ProfissionalResumoResponse;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.DisponibilidadeProfissional;
import br.com.clinicaviene.entity.ExcecaoAgenda;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusAgendamento;
import br.com.clinicaviene.enums.StatusUsuario;
import br.com.clinicaviene.enums.TipoExcecaoAgenda;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.AgendamentoRepository;
import br.com.clinicaviene.repository.DisponibilidadeProfissionalRepository;
import br.com.clinicaviene.repository.ExcecaoAgendaRepository;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.DisponibilidadeCalculo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Horários dos profissionais (D-106): fixos semanais + horários extras − ausências. A recepção (ADMIN) e o MASTER
 * cadastram o que o profissional passar; o profissional vê os próprios. Fora desses horários não se marca consulta.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private static final String ENTIDADE = "Profissional";
    /** Limite de dias numa consulta de disponibilidade (calendário de até um mês e pouco). */
    private static final int MAXIMO_DIAS = 62;
    /** "Sem fim" nas consultas ao banco (LocalDate.MAX não cabe numa coluna DATE). */
    private static final LocalDate SEM_FIM = LocalDate.of(9999, 12, 31);

    private final DisponibilidadeProfissionalRepository disponibilidadeRepository;
    private final ExcecaoAgendaRepository excecaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConsultorioService consultorioService;
    private final AuditoriaService auditoriaService;
    private final AgendaProperties agenda;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Usuario> profissionais(UsuarioAutenticado ator) {
        if (!AgendaPermissoes.usaAgenda(ator.perfil())) {
            throw new AccessDeniedException("Sem acesso à agenda");
        }
        return usuarioRepository.findAllByPerfilAndStatusOrderByNomeCompletoAsc(Perfil.PROFISSIONAL, StatusUsuario.ATIVO);
    }

    @Transactional(readOnly = true)
    public HorariosProfissionalResponse horarios(UUID profissionalId, UsuarioAutenticado ator) {
        exigirLeitura(profissionalId, ator);
        Usuario profissional = profissional(profissionalId);
        LocalDate hoje = hoje();
        Instant inicioHoje = hoje.atStartOfDay(agenda.fuso()).toInstant();
        return new HorariosProfissionalResponse(
                ProfissionalResumoResponse.from(profissional),
                disponibilidadeRepository.vigentesAPartirDe(profissionalId, hoje).stream()
                        .map(HorarioFixoResponse::from)
                        .toList(),
                excecaoRepository.ativasEntre(profissionalId, inicioHoje, inicioHoje.plus(3650, ChronoUnit.DAYS)).stream()
                        .map(ExcecaoAgendaResponse::from)
                        .toList());
    }

    @Transactional
    public DisponibilidadeProfissional criarFixo(UUID profissionalId, NovoHorarioFixoRequest pedido, UsuarioAutenticado ator) {
        exigirGestao(ator);
        profissional(profissionalId);
        consultorioService.ativo(pedido.consultorioId());
        if (!pedido.horaFim().isAfter(pedido.horaInicio())) {
            throw new RegraNegocioException("O horário de fim precisa ser depois do início.");
        }
        exigirNaGrade(pedido.horaInicio());
        exigirNaGrade(pedido.horaFim());
        if (pedido.vigenciaFim() != null && pedido.vigenciaFim().isBefore(pedido.vigenciaInicio())) {
            throw new RegraNegocioException("O fim da vigência precisa ser depois do início.");
        }
        LocalDate ate = pedido.vigenciaFim() == null ? SEM_FIM : pedido.vigenciaFim();
        boolean sobrepoe = disponibilidadeRepository.vigentesEntre(profissionalId, pedido.vigenciaInicio(), ate).stream()
                .anyMatch(d -> d.dia() == pedido.diaSemana()
                        && d.getHoraInicio().isBefore(pedido.horaFim())
                        && pedido.horaInicio().isBefore(d.getHoraFim()));
        if (sobrepoe) {
            throw new RegraNegocioException("O profissional já tem horário fixo que se sobrepõe a este no mesmo dia da semana.");
        }
        DisponibilidadeProfissional d = disponibilidadeRepository.save(new DisponibilidadeProfissional(
                profissionalId, pedido.consultorioId(), pedido.diaSemana(), pedido.horaInicio(), pedido.horaFim(),
                pedido.vigenciaInicio(), pedido.vigenciaFim(), ator.id(), agora()));
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.DISPONIBILIDADE_CRIADA, ENTIDADE, profissionalId.toString(), "horário fixo " + d.getId()));
        return d;
    }

    /**
     * O horário fixo deixa de valer depois de {@code ultimoDia}. Se ainda nem começou a valer, é removido (foi
     * cadastrado por engano).
     */
    @Transactional
    public void encerrarFixo(UUID profissionalId, UUID horarioId, LocalDate ultimoDia, UsuarioAutenticado ator) {
        exigirGestao(ator);
        DisponibilidadeProfissional d = disponibilidadeRepository.findById(horarioId)
                .filter(h -> h.getProfissionalId().equals(profissionalId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Horário não encontrado."));
        if (ultimoDia.isBefore(d.getVigenciaInicio())) {
            if (!d.getVigenciaInicio().isAfter(hoje())) {
                throw new RegraNegocioException("Este horário já está em vigor: informe um último dia a partir do início.");
            }
            disponibilidadeRepository.delete(d);
        } else {
            d.encerrar(ultimoDia);
        }
        exigirConsultasDentroDosHorarios(profissionalId);
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.DISPONIBILIDADE_ENCERRADA, ENTIDADE, profissionalId.toString(), "horário fixo " + horarioId));
    }

    @Transactional
    public ExcecaoAgenda criarExcecao(UUID profissionalId, NovaExcecaoAgendaRequest pedido, UsuarioAutenticado ator) {
        exigirGestao(ator);
        profissional(profissionalId);
        if (!pedido.fim().isAfter(pedido.inicio())) {
            throw new RegraNegocioException("O fim precisa ser depois do início.");
        }
        if (pedido.consultorioId() != null) {
            consultorioService.ativo(pedido.consultorioId());
        }
        if (pedido.tipo() == TipoExcecaoAgenda.HORARIO_EXTRA) {
            if (pedido.consultorioId() == null) {
                throw new RegraNegocioException("Informe o consultório do horário extra.");
            }
            exigirNaGrade(pedido.inicio().atZone(agenda.fuso()).toLocalTime());
            exigirNaGrade(pedido.fim().atZone(agenda.fuso()).toLocalTime());
            boolean sobrepoe = excecaoRepository.ativasEntre(profissionalId, pedido.inicio(), pedido.fim()).stream()
                    .anyMatch(e -> e.getTipo() == TipoExcecaoAgenda.HORARIO_EXTRA);
            if (sobrepoe) {
                throw new RegraNegocioException("Já existe um horário extra deste profissional neste período.");
            }
        }
        String motivo = pedido.motivo() == null || pedido.motivo().isBlank() ? null : pedido.motivo().trim();
        ExcecaoAgenda e = excecaoRepository.saveAndFlush(new ExcecaoAgenda(
                profissionalId, pedido.consultorioId(), pedido.tipo(), pedido.inicio(), pedido.fim(), motivo, ator.id(), agora()));
        if (pedido.tipo() == TipoExcecaoAgenda.AUSENCIA) {
            exigirConsultasDentroDosHorarios(profissionalId);
        }
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.EXCECAO_AGENDA_CRIADA, ENTIDADE, profissionalId.toString(), pedido.tipo() + " " + e.getId()));
        return e;
    }

    @Transactional
    public void cancelarExcecao(UUID profissionalId, UUID excecaoId, UsuarioAutenticado ator) {
        exigirGestao(ator);
        ExcecaoAgenda e = excecaoRepository.findById(excecaoId)
                .filter(x -> x.getProfissionalId().equals(profissionalId) && x.ativa())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exceção não encontrada."));
        e.cancelar(ator.id(), agora());
        if (e.getTipo() == TipoExcecaoAgenda.HORARIO_EXTRA) {
            excecaoRepository.flush();
            exigirConsultasDentroDosHorarios(profissionalId);
        }
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.EXCECAO_AGENDA_CANCELADA, ENTIDADE, profissionalId.toString(), e.getTipo() + " " + excecaoId));
    }

    /** Trechos em que o profissional atende entre os dias {@code de} e {@code ate} (inclusive, hora local). */
    @Transactional(readOnly = true)
    public List<IntervaloDisponivelDto> disponiveis(UUID profissionalId, LocalDate de, LocalDate ate, UsuarioAutenticado ator) {
        exigirLeitura(profissionalId, ator);
        if (ate.isBefore(de) || ChronoUnit.DAYS.between(de, ate) > MAXIMO_DIAS) {
            throw new RegraNegocioException("Período inválido (máximo de " + MAXIMO_DIAS + " dias).");
        }
        return calcular(profissionalId, de, ate);
    }

    /** O profissional atende nesse consultório durante todo o período? (usado ao marcar consulta) */
    @Transactional(readOnly = true)
    public boolean atendeNoPeriodo(UUID profissionalId, UUID consultorioId, Instant inicio, Instant fim) {
        LocalDate de = inicio.atZone(agenda.fuso()).toLocalDate();
        LocalDate ate = fim.atZone(agenda.fuso()).toLocalDate();
        return DisponibilidadeCalculo.cabe(calcular(profissionalId, de, ate), consultorioId, inicio, fim);
    }

    /**
     * Depois de reduzir os horários (encerrar fixo, ausência, cancelar extra), nenhuma consulta futura em aberto pode
     * ficar fora deles: a recepção remarca ou cancela antes. A transação inteira é desfeita.
     */
    private void exigirConsultasDentroDosHorarios(UUID profissionalId) {
        disponibilidadeRepository.flush();
        long fora = agendamentoRepository
                .findByProfissionalIdAndStatusInAndInicioGreaterThanEqual(
                        profissionalId, List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO), agora())
                .stream()
                .filter(a -> !atendeNoPeriodo(profissionalId, a.getConsultorioId(), a.getInicio(), a.getFim()))
                .count();
        if (fora > 0) {
            throw new RegraNegocioException("Há " + fora + " consulta(s) marcada(s) que ficariam fora dos horários do"
                    + " profissional. Remarque ou cancele antes.");
        }
    }

    private List<IntervaloDisponivelDto> calcular(UUID profissionalId, LocalDate de, LocalDate ate) {
        Instant ini = de.atStartOfDay(agenda.fuso()).toInstant();
        Instant fim = ate.plusDays(1).atStartOfDay(agenda.fuso()).toInstant();
        return DisponibilidadeCalculo.calcular(
                de, ate, agenda.fuso(),
                disponibilidadeRepository.vigentesEntre(profissionalId, de, ate),
                excecaoRepository.ativasEntre(profissionalId, ini, fim));
    }

    private void exigirNaGrade(LocalTime hora) {
        long minutos = hora.toSecondOfDay() / 60;
        if (hora.getSecond() != 0 || hora.getNano() != 0 || minutos % agenda.grade().toMinutes() != 0) {
            throw new RegraNegocioException(
                    "Os horários precisam ser de " + agenda.grade().toMinutes() + " em " + agenda.grade().toMinutes() + " minutos.");
        }
    }

    /** Profissional ativo (perfil PROFISSIONAL). */
    @Transactional(readOnly = true)
    public Usuario profissional(UUID id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getPerfil() == Perfil.PROFISSIONAL && u.getStatus() == StatusUsuario.ATIVO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado ou inativo."));
    }

    private static void exigirGestao(UsuarioAutenticado ator) {
        if (!AgendaPermissoes.gerenciaAgenda(ator.perfil())) {
            throw new AccessDeniedException("Só a recepção e o MASTER cadastram horários");
        }
    }

    /** Recepção e MASTER veem todos; o profissional, só os próprios. */
    private static void exigirLeitura(UUID profissionalId, UsuarioAutenticado ator) {
        boolean proprio = ator.perfil() == Perfil.PROFISSIONAL && ator.id().equals(profissionalId);
        if (!AgendaPermissoes.gerenciaAgenda(ator.perfil()) && !proprio) {
            throw new AccessDeniedException("Sem acesso aos horários deste profissional");
        }
    }

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(agenda.fuso()));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
