package br.com.clinicaviene.entity;

import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.enums.AcaoAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Linha do {@code audit_log}. Imutável: o banco rejeita UPDATE/DELETE (D-076). */
@Entity
@Immutable
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 80)
    private AcaoAuditoria acao;

    @Column(updatable = false, length = 80)
    private String entidade;

    @Column(name = "entidade_id", updatable = false, length = 80)
    private String entidadeId;

    @Column(updatable = false, length = 45)
    private String ip;

    @Column(updatable = false)
    private String justificativa;

    public AuditLog(RegistroAuditoria registro, String ip, Instant ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
        this.usuarioId = registro.usuarioId();
        this.acao = registro.acao();
        this.entidade = registro.entidade();
        this.entidadeId = registro.entidadeId();
        this.ip = ip;
        this.justificativa = registro.justificativa();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditLog outra)) {
            return false;
        }
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return AuditLog.class.hashCode();
    }
}
