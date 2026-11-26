package br.com.clinicaviene.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Código de recuperação do 2FA (D-072): uso único, só o hash fica no banco. */
@Entity
@Table(name = "codigo_recuperacao_2fa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodigoRecuperacao {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "codigo_hash", nullable = false, length = 64, updatable = false)
    private String codigoHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    public CodigoRecuperacao(UUID usuarioId, String codigoHash, Instant agora) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.codigoHash = codigoHash;
        this.criadoEm = agora;
    }

    public void marcarUsado(Instant agora) {
        this.usadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CodigoRecuperacao outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return CodigoRecuperacao.class.hashCode();
    }
}
