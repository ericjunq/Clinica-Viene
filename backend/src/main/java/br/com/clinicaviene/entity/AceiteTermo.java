package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.DecisaoTermo;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Uma decisão do paciente sobre um termo (aceitou, recusou, revogou), com as evidências (D-093). Somente
 * inclusão: o estado atual de cada consentimento é a decisão mais recente.
 */
@Entity
@Immutable
@Table(name = "aceite_termo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AceiteTermo {

    @Id
    private UUID id;

    @Column(name = "termo_versao_id", nullable = false, updatable = false)
    private UUID termoVersaoId;

    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private TipoTermo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private DecisaoTermo decisao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private OrigemAlteracao origem;

    @Column(name = "pelo_responsavel", nullable = false, updatable = false)
    private boolean peloResponsavel;

    @Column(length = 45, updatable = false)
    private String ip;

    @Column(length = 300, updatable = false)
    private String dispositivo;

    @Column(name = "registrado_por", updatable = false)
    private UUID registradoPor;

    @Column(updatable = false)
    private String justificativa;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    public AceiteTermo(
            TermoVersao termo,
            UUID pacienteId,
            DecisaoTermo decisao,
            OrigemAlteracao origem,
            boolean peloResponsavel,
            String ip,
            String dispositivo,
            UUID registradoPor,
            String justificativa,
            Instant agora) {
        this.id = IdUtils.novoId();
        this.termoVersaoId = termo.getId();
        this.tipo = termo.getTipo();
        this.pacienteId = pacienteId;
        this.decisao = decisao;
        this.origem = origem;
        this.peloResponsavel = peloResponsavel;
        this.ip = ip;
        this.dispositivo = dispositivo == null || dispositivo.length() <= 300 ? dispositivo : dispositivo.substring(0, 300);
        this.registradoPor = registradoPor;
        this.justificativa = justificativa;
        this.registradoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AceiteTermo outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return AceiteTermo.class.hashCode();
    }
}
