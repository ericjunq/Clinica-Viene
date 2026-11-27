package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.TipoExcecaoAgenda;
import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Horário extra ou ausência pontual de um profissional (D-106). Cancelar mantém o registro. */
@Entity
@Table(name = "excecao_agenda")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExcecaoAgenda {

    @Id
    private UUID id;

    @Column(name = "profissional_id", nullable = false, updatable = false)
    private UUID profissionalId;

    /** Obrigatório no horário extra; na ausência, vazio = vale para todos os consultórios. */
    @Column(name = "consultorio_id", updatable = false)
    private UUID consultorioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoExcecaoAgenda tipo;

    @Column(nullable = false, updatable = false)
    private Instant inicio;

    @Column(nullable = false, updatable = false)
    private Instant fim;

    @Column(length = 300, updatable = false)
    private String motivo;

    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "cancelada_por")
    private UUID canceladaPor;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    public ExcecaoAgenda(
            UUID profissionalId,
            UUID consultorioId,
            TipoExcecaoAgenda tipo,
            Instant inicio,
            Instant fim,
            String motivo,
            UUID criadoPor,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.profissionalId = profissionalId;
        this.consultorioId = consultorioId;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fim = fim;
        this.motivo = motivo;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public boolean ativa() {
        return canceladaEm == null;
    }

    public void cancelar(UUID por, Instant agora) {
        this.canceladaPor = por;
        this.canceladaEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ExcecaoAgenda outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return ExcecaoAgenda.class.hashCode();
    }
}
