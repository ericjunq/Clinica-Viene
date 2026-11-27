package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Link de download de um documento (D-094): expira, tem limite de downloads e de CPFs errados. */
@Entity
@Table(name = "entrega_documento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntregaDocumento {

    @Id
    private UUID id;

    @Column(name = "documento_id", nullable = false, updatable = false)
    private UUID documentoId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private int downloads;

    @Column(name = "tentativas_cpf", nullable = false)
    private int tentativasCpf;

    @Column(name = "ultimo_download_em")
    private Instant ultimoDownloadEm;

    public EntregaDocumento(UUID documentoId, String tokenHash, Instant agora, Instant expiraEm) {
        this.id = IdUtils.novoId();
        this.documentoId = documentoId;
        this.tokenHash = tokenHash;
        this.criadaEm = agora;
        this.expiraEm = expiraEm;
    }

    public boolean utilizavel(Instant agora, int maximoDownloads, int maximoTentativasCpf) {
        return agora.isBefore(expiraEm) && downloads < maximoDownloads && tentativasCpf < maximoTentativasCpf;
    }

    public void registrarCpfIncorreto() {
        tentativasCpf++;
    }

    public void registrarDownload(Instant agora) {
        downloads++;
        ultimoDownloadEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof EntregaDocumento outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return EntregaDocumento.class.hashCode();
    }
}
