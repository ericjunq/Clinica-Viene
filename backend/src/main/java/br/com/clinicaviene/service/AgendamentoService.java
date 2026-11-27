package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AgendaPermissoes;
import br.com.clinicaviene.config.AgendaProperties;
import br.com.clinicaviene.dto.AgendamentoResponse;
import br.com.clinicaviene.dto.ItemAgendamentoRequest;
import br.com.clinicaviene.dto.NovoAgendamentoRequest;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.RemarcarAgendamentoRequest;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Agendamento;
import br.com.clinicaviene.entity.CategoriaProcedimento;
import br.com.clinicaviene.entity.Consultorio;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusAgendamento;
import br.com.clinicaviene.exceptions.ConflitoAgendaException;
import br.com.clinicaviene.exceptions.PacienteNaoEncontradoException;
import br.com.clinicaviene.exceptions.RecursoNaoEncontradoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.AgendamentoRepository;
import br.com.clinicaviene.repository.CategoriaProcedimentoRepository;
import br.com.clinicaviene.repository.ConsultorioRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import br.com.clinicaviene.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agendamentos (CONTEXTO §7, D-106). Só se marca dentro dos horários do profissional; conflito de profissional ou
 * consultório é barrado aqui e, para corridas entre duas recepcionistas, no banco (D-021).
 */
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final String ENTIDADE = "Agendamento";
    private static final int MAXIMO_DIAS = 62;
    private static final Duration DURACAO_MAXIMA = Duration.ofHours(12);
    private static final UUID NENHUM = new UUID(0, 0);

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConsultorioRepository consultorioRepository;
    private final CategoriaProcedimentoRepository categoriaRepository;
    private final DisponibilidadeService disponibilidadeService;
    private final ConsultorioService consultorioService;
    private final CategoriaProcedimentoService categoriaService;
    private final DocumentoService documentoService;
    private final AuditoriaService auditoriaService;
    private final AgendaProperties agenda;
    private final Clock clock;

    @Transactional
    public AgendamentoResponse criar(NovoAgendamentoRequest pedido, UsuarioAutenticado ator) {
        exigirGestao(ator);
        if (!pacienteRepository.existsById(pedido.pacienteId())) {
            throw new PacienteNaoEncontradoException();
        }
        Duration duracao = duracao(pedido.duracaoMinutos(), agenda.duracaoPadrao());
        Agendamento novo = marcar(
                pedido.pacienteId(), pedido.profissionalId(), pedido.consultorioId(), pedido.inicio(), duracao,
                itens(pedido.itens(), pedido.profissionalId()), texto(pedido.observacao()), null, ator);
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.AGENDAMENTO_CRIADO, ENTIDADE, novo.getId()));
        return respostas(List.of(novo)).getFirst();
    }

    /**
     * Agenda entre os dias {@code de} e {@code ate} (inclusive, hora local). O profissional só vê a própria. Não é
     * somente leitura: registra a consulta na auditoria.
     */
    @Transactional
    public List<AgendamentoResponse> listar(
            LocalDate de, LocalDate ate, UUID profissionalId, UUID consultorioId, UsuarioAutenticado ator) {
        if (!AgendaPermissoes.usaAgenda(ator.perfil())) {
            throw new AccessDeniedException("Sem acesso à agenda");
        }
        if (ator.perfil() == Perfil.PROFISSIONAL) {
            if (profissionalId != null && !profissionalId.equals(ator.id())) {
                throw new AccessDeniedException("O profissional só vê a própria agenda");
            }
            profissionalId = ator.id();
        }
        if (ate.isBefore(de) || ChronoUnit.DAYS.between(de, ate) > MAXIMO_DIAS) {
            throw new RegraNegocioException("Período inválido (máximo de " + MAXIMO_DIAS + " dias).");
        }
        UUID profissional = profissionalId;
        List<Agendamento> lista = agendamentoRepository
                .doPeriodo(de.atStartOfDay(agenda.fuso()).toInstant(), ate.plusDays(1).atStartOfDay(agenda.fuso()).toInstant())
                .stream()
                .filter(a -> profissional == null || a.getProfissionalId().equals(profissional))
                .filter(a -> consultorioId == null || a.getConsultorioId().equals(consultorioId))
                .toList();
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.AGENDA_CONSULTADA, ENTIDADE, null, de + " a " + ate));
        return respostas(lista);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(UUID id, UsuarioAutenticado ator) {
        Agendamento a = agendamento(id);
        exigirLeitura(a, ator);
        return respostas(List.of(a)).getFirst();
    }

    @Transactional
    public AgendamentoResponse alterarItens(UUID id, List<ItemAgendamentoRequest> itens, String observacao, UsuarioAutenticado ator) {
        exigirGestao(ator);
        Agendamento a = agendamento(id);
        if (!a.getStatus().emAberto()) {
            throw new RegraNegocioException("Só é possível alterar consultas ainda não atendidas.");
        }
        List<Agendamento.ItemNovo> novos = itens(itens, a.getProfissionalId());
        // apaga os itens antigos antes de gravar os novos (o Hibernate inseriria antes de apagar e a ordem repetiria)
        a.definirItens(List.of(), texto(observacao), agora());
        agendamentoRepository.flush();
        a.definirItens(novos, texto(observacao), agora());
        auditoriaService.registrar(RegistroAuditoria.sobre(ator.id(), AcaoAuditoria.AGENDAMENTO_ALTERADO, ENTIDADE, id));
        return respostas(List.of(a)).getFirst();
    }

    /**
     * Confirmar, chegada (check-in) e falta: recepção/MASTER. Início e fim do atendimento: também o próprio
     * profissional.
     */
    @Transactional
    public AgendamentoResponse mudarStatus(UUID id, StatusAgendamento novo, UsuarioAutenticado ator) {
        if (novo == StatusAgendamento.CANCELADO || novo == StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Use cancelar ou remarcar.");
        }
        Agendamento a = agendamento(id);
        boolean doProprio = ator.perfil() == Perfil.PROFISSIONAL && a.getProfissionalId().equals(ator.id());
        boolean atendimento = novo == StatusAgendamento.EM_ATENDIMENTO || novo == StatusAgendamento.CONCLUIDO;
        if (!AgendaPermissoes.gerenciaAgenda(ator.perfil()) && !(atendimento && doProprio)) {
            throw new AccessDeniedException("Sem permissão para mudar esta consulta");
        }
        a.mudarStatus(novo, agora());
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.AGENDAMENTO_STATUS_ALTERADO, ENTIDADE, id.toString(), novo.name()));
        return respostas(List.of(a)).getFirst();
    }

    @Transactional
    public AgendamentoResponse cancelar(UUID id, String motivo, UsuarioAutenticado ator) {
        exigirGestao(ator);
        Agendamento a = agendamento(id);
        a.cancelar(motivo.trim(), ator.id(), agora());
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.AGENDAMENTO_CANCELADO, ENTIDADE, id.toString(), motivo.trim()));
        return respostas(List.of(a)).getFirst();
    }

    /** Cancela este agendamento ("Remarcado: motivo") e cria outro ligado a ele, com os mesmos itens. */
    @Transactional
    public AgendamentoResponse remarcar(UUID id, RemarcarAgendamentoRequest pedido, UsuarioAutenticado ator) {
        exigirGestao(ator);
        Agendamento antigo = agendamento(id);
        if (!antigo.getStatus().emAberto()) {
            throw new RegraNegocioException("Só é possível remarcar consultas ainda não atendidas.");
        }
        antigo.cancelar("Remarcado: " + pedido.motivo().trim(), ator.id(), agora());
        agendamentoRepository.flush();
        UUID profissional = pedido.profissionalId() == null ? antigo.getProfissionalId() : pedido.profissionalId();
        UUID consultorio = pedido.consultorioId() == null ? antigo.getConsultorioId() : pedido.consultorioId();
        Duration duracao = duracao(pedido.duracaoMinutos(), Duration.between(antigo.getInicio(), antigo.getFim()));
        List<ItemAgendamentoRequest> mesmosItens = antigo.getItens().stream()
                .map(i -> new ItemAgendamentoRequest(i.getCategoriaId(), i.getDescricao()))
                .toList();
        Agendamento novo = marcar(
                antigo.getPacienteId(), profissional, consultorio, pedido.inicio(), duracao,
                itens(mesmosItens, profissional), antigo.getObservacao(), antigo.getId(), ator);
        auditoriaService.registrar(new RegistroAuditoria(
                ator.id(), AcaoAuditoria.AGENDAMENTO_REMARCADO, ENTIDADE, id.toString(), "novo agendamento " + novo.getId()));
        return respostas(List.of(novo)).getFirst();
    }

    // --- regras de marcação ---

    private Agendamento marcar(
            UUID pacienteId,
            UUID profissionalId,
            UUID consultorioId,
            Instant inicio,
            Duration duracao,
            List<Agendamento.ItemNovo> itens,
            String observacao,
            UUID origemId,
            UsuarioAutenticado ator) {
        disponibilidadeService.profissional(profissionalId);
        consultorioService.ativo(consultorioId);
        Instant fim = inicio.plus(duracao);
        if (inicio.isBefore(agora())) {
            throw new RegraNegocioException("Não é possível marcar consulta num horário que já passou.");
        }
        long grade = agenda.grade().toMinutes();
        var local = inicio.atZone(agenda.fuso());
        if (local.getSecond() != 0 || local.getNano() != 0 || local.getMinute() % grade != 0 || duracao.toMinutes() % grade != 0) {
            throw new RegraNegocioException("Início e duração precisam ser de " + grade + " em " + grade + " minutos.");
        }
        if (!disponibilidadeService.atendeNoPeriodo(profissionalId, consultorioId, inicio, fim)) {
            throw new RegraNegocioException(
                    "O profissional não atende neste consultório nesse horário. Para encaixar, cadastre antes um horário extra.");
        }
        List<Agendamento> conflitos = agendamentoRepository.conflitantes(
                inicio, fim, profissionalId, consultorioId, origemId == null ? NENHUM : origemId);
        if (conflitos.stream().anyMatch(c -> c.getProfissionalId().equals(profissionalId))) {
            throw new ConflitoAgendaException("O profissional já tem consulta neste horário.");
        }
        if (!conflitos.isEmpty()) {
            throw new ConflitoAgendaException("O consultório já está ocupado neste horário.");
        }
        Agendamento a = new Agendamento(pacienteId, profissionalId, consultorioId, inicio, fim, observacao, origemId, ator.id(), agora());
        a.definirItens(itens, observacao, agora());
        try {
            return agendamentoRepository.saveAndFlush(a);
        } catch (DataIntegrityViolationException corrida) {
            // outra pessoa marcou o mesmo horário entre a conferência e a gravação (EXCLUDE no banco — D-021)
            throw new ConflitoAgendaException("Este horário acabou de ser ocupado. Atualize a agenda e escolha outro.");
        }
    }

    /** Categorias ativas e da área do profissional. */
    private List<Agendamento.ItemNovo> itens(List<ItemAgendamentoRequest> pedidos, UUID profissionalId) {
        Usuario profissional = disponibilidadeService.profissional(profissionalId);
        return pedidos.stream()
                .map(p -> {
                    CategoriaProcedimento c = categoriaService.ativa(p.categoriaId());
                    if (c.getAreaAtuacao() != profissional.getAreaAtuacao()) {
                        throw new RegraNegocioException("A categoria \"" + c.getNome() + "\" não é da área do profissional.");
                    }
                    return new Agendamento.ItemNovo(c.getId(), texto(p.descricao()));
                })
                .toList();
    }

    private Duration duracao(Integer minutos, Duration padrao) {
        Duration d = minutos == null ? padrao : Duration.ofMinutes(minutos);
        if (d.isZero() || d.isNegative() || d.compareTo(DURACAO_MAXIMA) > 0) {
            throw new RegraNegocioException("Duração inválida.");
        }
        return d;
    }

    // --- respostas ---

    private List<AgendamentoResponse> respostas(List<Agendamento> lista) {
        Set<UUID> pacientes = lista.stream().map(Agendamento::getPacienteId).collect(Collectors.toSet());
        Set<UUID> usuarios = lista.stream().map(Agendamento::getProfissionalId).collect(Collectors.toSet());
        Set<UUID> consultorios = lista.stream().map(Agendamento::getConsultorioId).collect(Collectors.toSet());
        Set<UUID> categorias = new HashSet<>();
        lista.forEach(a -> a.getItens().forEach(i -> categorias.add(i.getCategoriaId())));
        Map<UUID, Paciente> mapaPacientes = porId(pacienteRepository.findAllById(pacientes), Paciente::getId);
        Map<UUID, Usuario> mapaUsuarios = porId(usuarioRepository.findAllById(usuarios), Usuario::getId);
        Map<UUID, Consultorio> mapaConsultorios = porId(consultorioRepository.findAllById(consultorios), Consultorio::getId);
        Map<UUID, CategoriaProcedimento> mapaCategorias =
                porId(categoriaRepository.findAllById(categorias), CategoriaProcedimento::getId);
        Set<UUID> comFicha = documentoService.comFichaAssinada(pacientes);
        return lista.stream()
                .map(a -> new AgendamentoResponse(
                        a.getId(),
                        new AgendamentoResponse.Paciente(
                                a.getPacienteId(), mapaPacientes.get(a.getPacienteId()).getNomeCompleto(),
                                !comFicha.contains(a.getPacienteId())),
                        new AgendamentoResponse.Referencia(
                                a.getProfissionalId(), mapaUsuarios.get(a.getProfissionalId()).getNomeCompleto()),
                        new AgendamentoResponse.Referencia(
                                a.getConsultorioId(), mapaConsultorios.get(a.getConsultorioId()).getNome()),
                        a.getInicio(),
                        a.getFim(),
                        a.getStatus(),
                        a.getObservacao(),
                        a.getItens().stream()
                                .map(i -> {
                                    CategoriaProcedimento c = mapaCategorias.get(i.getCategoriaId());
                                    return new AgendamentoResponse.Item(c.getId(), c.getCodigo(), c.getNome(), i.getDescricao());
                                })
                                .toList(),
                        a.getAgendamentoOrigemId(),
                        a.getMotivoCancelamento(),
                        a.getChegadaEm(),
                        a.getCriadoEm()))
                .toList();
    }

    private static <T> Map<UUID, T> porId(Collection<T> itens, Function<T, UUID> id) {
        return itens.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    // --- apoio ---

    private Agendamento agendamento(UUID id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
    }

    private static void exigirGestao(UsuarioAutenticado ator) {
        if (!AgendaPermissoes.gerenciaAgenda(ator.perfil())) {
            throw new AccessDeniedException("Só a recepção e o MASTER marcam consultas");
        }
    }

    private static void exigirLeitura(Agendamento a, UsuarioAutenticado ator) {
        boolean doProprio = ator.perfil() == Perfil.PROFISSIONAL && a.getProfissionalId().equals(ator.id());
        if (!AgendaPermissoes.gerenciaAgenda(ator.perfil()) && !doProprio) {
            throw new AccessDeniedException("Sem acesso a esta consulta");
        }
    }

    private static String texto(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
