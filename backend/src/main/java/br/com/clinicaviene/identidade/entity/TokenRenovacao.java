package br.com.clinicaviene.identidade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Token de renovação de sessão (D-071). Só o hash do valor fica no banco. */
@Entity
@Table(name = "token_renovacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenRenovacao {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "cadeia_id", nullable = false, updatable = false)
    private UUID cadeiaId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** Fim da sessão (absoluto, contado do login): renovar não estende. */
    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    public TokenRenovacao(UUID usuarioId, UUID cadeiaId, String tokenHash, Instant criadoEm, Instant expiraEm) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.cadeiaId = cadeiaId;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean expirado(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    public boolean revogado() {
        return revogadoEm != null;
    }

    public boolean usado() {
        return usadoEm != null;
    }

    public void marcarUsado(Instant agora) {
        this.usadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenRenovacao outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return TokenRenovacao.class.hashCode();
    }
}
