package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.StompAutenticacaoInterceptor;
import br.com.clinicaviene.dto.AgendaAlteradaDto;
import br.com.clinicaviene.dto.AvisoChegadaDto;
import br.com.clinicaviene.entity.Agendamento;
import br.com.clinicaviene.repository.AgendamentoRepository;
import br.com.clinicaviene.repository.ConsultorioRepository;
import br.com.clinicaviene.repository.PacienteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia os avisos em tempo real depois que a mudança foi gravada (D-108): nada é avisado se a transação falhar.
 * Quem não está conectado não recebe (a agenda mostra a situação ao abrir).
 *
 * <p>Os eventos levam só ids e datas: o Spring Modulith guarda os eventos publicados (e os arquiva — D-059), então
 * nome de paciente nunca vai neles; é lido aqui, na hora de enviar.
 */
@Service
@RequiredArgsConstructor
public class AvisosAgendaService {

    /** Evento publicado pelo {@link AgendamentoService} no check-in. */
    public record ChegadaRegistrada(UUID agendamentoId) {}

    private final SimpMessagingTemplate mensagens;
    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ConsultorioRepository consultorioRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void agendaAlterada(AgendaAlteradaDto evento) {
        mensagens.convertAndSend(StompAutenticacaoInterceptor.AGENDA, evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void pacienteChegou(ChegadaRegistrada evento) {
        agendamentoRepository.findById(evento.agendamentoId()).ifPresent(this::avisarProfissional);
    }

    private void avisarProfissional(Agendamento a) {
        String paciente = pacienteRepository.findById(a.getPacienteId()).map(p -> p.getNomeCompleto()).orElse("");
        String consultorio = consultorioRepository.findById(a.getConsultorioId()).map(c -> c.getNome()).orElse("");
        mensagens.convertAndSendToUser(
                a.getProfissionalId().toString(),
                "/queue/avisos",
                new AvisoChegadaDto(
                        AvisoChegadaDto.PACIENTE_CHEGOU, a.getId(), paciente, consultorio, a.getInicio(), a.getChegadaEm()));
    }
}
