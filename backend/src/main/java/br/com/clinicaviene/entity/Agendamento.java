package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.StatusAgendamento;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Consulta marcada (CONTEXTO §7, D-106). Conflitos de horário são barrados no banco (D-021). Nunca é apagado:
 * cancelar guarda motivo e quem cancelou; remarcar = cancelar + novo agendamento ligado a este.
 */
@Entity
@Table(name = "agendamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agendamento {

    @Id
    private UUID id;

    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;

    @Column(name = "profissional_id", nullable = false, updatable = false)
    private UUID profissionalId;

    @Column(name = "consultorio_id", nullable = false, updatable = false)
    private UUID consultorioId;

    @Column(nullable = false, updatable = false)
    private Instant inicio;

    @Column(nullable = false, updatable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status;

    @Column(length = 500)
    private String observacao;

    @Column(name = "agendamento_origem_id", updatable = false)
    private UUID agendamentoOrigemId;

    @Column(name = "motivo_cancelamento", length = 300)
    private String motivoCancelamento;

    @Column(name = "cancelado_por")
    private UUID canceladoPor;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    @Column(name = "chegada_em")
    private Instant chegadaEm;

    @Column(name = "atendimento_inicio_em")
    private Instant atendimentoInicioEm;

    @Column(name = "atendimento_fim_em")
    private Instant atendimentoFimEm;

    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private long versao;

    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<AgendamentoItem> itens = new ArrayList<>();

    public Agendamento(
            UUID pacienteId,
            UUID profissionalId,
            UUID consultorioId,
            Instant inicio,
            Instant fim,
            String observacao,
            UUID agendamentoOrigemId,
            UUID criadoPor,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.consultorioId = consultorioId;
        this.inicio = inicio;
        this.fim = fim;
        this.status = StatusAgendamento.AGENDADO;
        this.observacao = observacao;
        this.agendamentoOrigemId = agendamentoOrigemId;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public List<AgendamentoItem> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /** Troca os itens (só enquanto agendado/confirmado). */
    public void definirItens(List<ItemNovo> novos, String observacao, Instant agora) {
        if (!itens.isEmpty() && !status.emAberto()) {
            throw new RegraNegocioException("Só é possível alterar agendamentos ainda não atendidos.");
        }
        itens.clear();
        for (int i = 0; i < novos.size(); i++) {
            itens.add(new AgendamentoItem(this, i + 1, novos.get(i).categoriaId(), novos.get(i).descricao()));
        }
        this.observacao = observacao;
        this.atualizadoEm = agora;
    }

    public void mudarStatus(StatusAgendamento novo, Instant agora) {
        if (!status.podeIrPara(novo)) {
            throw new RegraNegocioException("Não é possível passar de " + status + " para " + novo + ".");
        }
        switch (novo) {
            case PACIENTE_CHEGOU -> chegadaEm = agora;
            case EM_ATENDIMENTO -> atendimentoInicioEm = agora;
            case CONCLUIDO -> atendimentoFimEm = agora;
            default -> {
                // sem marca de tempo própria
            }
        }
        this.status = novo;
        this.atualizadoEm = agora;
    }

    public void cancelar(String motivo, UUID por, Instant agora) {
        mudarStatus(StatusAgendamento.CANCELADO, agora);
        this.motivoCancelamento = motivo;
        this.canceladoPor = por;
        this.canceladoEm = agora;
    }

    /** Item a gravar (categoria já conferida pelo service). */
    public record ItemNovo(UUID categoriaId, String descricao) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Agendamento outro && id != null && Objects.equals(id, outro.id);
    }

    @Override
    public int hashCode() {
        return Agendamento.class.hashCode();
    }
}
