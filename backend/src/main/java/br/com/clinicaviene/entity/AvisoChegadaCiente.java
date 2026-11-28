package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * O profissional da consulta confirmou que viu o aviso de chegada do paciente (D-111). Somente inclusão, um por
 * consulta.
 */
@Entity
@Immutable
@Table(name = "aviso_chegada_ciente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvisoChegadaCiente {

    @Id
    private UUID id;

    @Column(name = "agendamento_id", nullable = false, updatable = false)
    private UUID agendamentoId;

    @Column(name = "profissional_id", nullable = false, updatable = false)
    private UUID profissionalId;

    @Column(name = "ciente_em", nullable = false, updatable = false)
    private Instant cienteEm;

    public AvisoChegadaCiente(UUID agendamentoId, UUID profissionalId, Instant agora) {
        this.id = IdUtils.novoId();
        this.agendamentoId = agendamentoId;
        this.profissionalId = profissionalId;
        this.cienteEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AvisoChegadaCiente outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return AvisoChegadaCiente.class.hashCode();
    }
}
