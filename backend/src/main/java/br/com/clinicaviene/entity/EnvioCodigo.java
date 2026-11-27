package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import br.com.clinicaviene.enums.FinalidadeEnvio;
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

/** Um código enviado por e-mail (D-072, D-073). Uso único; morre ao expirar, ao ser substituído ou após N erros. */
@Entity
@Table(name = "envio_codigo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnvioCodigo {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private FinalidadeEnvio finalidade;

    @Column(name = "codigo_hash", nullable = false, length = 64, updatable = false)
    private String codigoHash;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "enviado_em", nullable = false, updatable = false)
    private Instant enviadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "invalidado_em")
    private Instant invalidadoEm;

    public EnvioCodigo(UUID usuarioId, FinalidadeEnvio finalidade, String codigoHash, Instant agora, Instant expiraEm) {
        this.id = IdUtils.novoId();
        this.usuarioId = usuarioId;
        this.finalidade = finalidade;
        this.codigoHash = codigoHash;
        this.tentativas = 0;
        this.enviadoEm = agora;
        this.expiraEm = expiraEm;
    }

    public boolean utilizavel(Instant agora) {
        return usadoEm == null && invalidadoEm == null && agora.isBefore(expiraEm);
    }

    public void usar(Instant agora) {
        this.usadoEm = agora;
    }

    public void invalidar(Instant agora) {
        if (invalidadoEm == null && usadoEm == null) {
            this.invalidadoEm = agora;
        }
    }

    /** Conta um código errado; ao atingir o máximo, o código é invalidado. */
    public void registrarFalha(int maximoTentativas, Instant agora) {
        this.tentativas++;
        if (tentativas >= maximoTentativas) {
            invalidar(agora);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvioCodigo outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return EnvioCodigo.class.hashCode();
    }
}
