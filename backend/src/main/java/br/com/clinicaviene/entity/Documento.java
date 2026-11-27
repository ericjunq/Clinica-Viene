package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.TipoDocumento;
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
import org.hibernate.annotations.Immutable;

/** PDF no storage privado (cifrado), referenciado pela chave; hash SHA-256 do PDF para provar integridade. */
@Entity
@Immutable
@Table(name = "documento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Documento {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumento tipo;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "chave_armazenamento", nullable = false, length = 300)
    private String chaveArmazenamento;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public Documento(UUID id, TipoDocumento tipo, UUID pacienteId, String chaveArmazenamento, String sha256, long tamanhoBytes, Instant agora) {
        this.id = id;
        this.tipo = tipo;
        this.pacienteId = pacienteId;
        this.chaveArmazenamento = chaveArmazenamento;
        this.sha256 = sha256;
        this.tamanhoBytes = tamanhoBytes;
        this.criadoEm = agora;
    }

    /** Id gerado antes do PDF, para a chave no storage já levar o id. */
    public static UUID novoId() {
        return IdUtils.novoId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Documento outro && id != null && Objects.equals(id, outro.id);
    }

    @Override
    public int hashCode() {
        return Documento.class.hashCode();
    }
}
