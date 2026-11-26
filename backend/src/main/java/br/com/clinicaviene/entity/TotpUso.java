package br.com.clinicaviene.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Último passo TOTP aceito do usuário: um código só vale uma vez (anti-replay). */
@Entity
@Table(name = "totp_uso")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TotpUso {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "ultimo_passo", nullable = false)
    private long ultimoPasso;

    public TotpUso(UUID usuarioId, long passo) {
        this.usuarioId = usuarioId;
        this.ultimoPasso = passo;
    }

    public void registrar(long passo) {
        this.ultimoPasso = passo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TotpUso outro)) {
            return false;
        }
        return usuarioId != null && usuarioId.equals(outro.usuarioId);
    }

    @Override
    public int hashCode() {
        return TotpUso.class.hashCode();
    }
}
