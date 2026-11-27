package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.SituacaoConvite;
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

/** Convite de cadastro (D-091): uso único, expira, só o hash do token no banco. */
@Entity
@Table(name = "convite_cadastro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConviteCadastro {

    @Id
    private UUID id;

    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private ModoConvite modo;

    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "enviado_por_email_em")
    private Instant enviadoPorEmailEm;

    /** Quiosque: recepcionista que conferiu o documento com foto do paciente. */
    @Column(name = "documento_conferido_por", updatable = false)
    private UUID documentoConferidoPor;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    public ConviteCadastro(
            UUID pacienteId, String tokenHash, ModoConvite modo, UUID criadoPor, UUID documentoConferidoPor, Instant agora,
            Instant expiraEm) {
        this.id = IdUtils.novoId();
        this.pacienteId = pacienteId;
        this.tokenHash = tokenHash;
        this.modo = modo;
        this.criadoPor = criadoPor;
        this.documentoConferidoPor = documentoConferidoPor;
        this.criadoEm = agora;
        this.expiraEm = expiraEm;
    }

    public SituacaoConvite situacao(Instant agora) {
        if (usadoEm != null) {
            return SituacaoConvite.USADO;
        }
        if (canceladoEm != null) {
            return SituacaoConvite.CANCELADO;
        }
        return agora.isBefore(expiraEm) ? SituacaoConvite.ATIVO : SituacaoConvite.EXPIRADO;
    }

    public void cancelar(Instant agora) {
        if (usadoEm == null && canceladoEm == null) {
            this.canceladoEm = agora;
        }
    }

    public void marcarEnviadoPorEmail(Instant agora) {
        this.enviadoPorEmailEm = agora;
    }

    public void marcarUsado(Instant agora) {
        this.usadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConviteCadastro outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return ConviteCadastro.class.hashCode();
    }
}
