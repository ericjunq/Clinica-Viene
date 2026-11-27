package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.enums.TipoSignatario;
import br.com.clinicaviene.util.CampoCriptografadoConverter;
import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

/** Assinatura eletrônica de um documento, com a trilha de evidências cifrada (D-090, D-102). */
@Entity
@Immutable
@Table(name = "assinatura")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assinatura {

    @Id
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(nullable = false, length = 30)
    private String provedor;

    @Column(name = "identificador_provedor", length = 120)
    private String identificadorProvedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_signatario", nullable = false, length = 30)
    private TipoSignatario tipoSignatario;

    @Column(name = "nome_signatario", nullable = false, length = 150)
    private String nomeSignatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetodoAssinatura metodo;

    /** JSON cifrado: IP, aparelho, como a identidade foi confirmada, hash do conteúdo. */
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(nullable = false)
    private String evidencias;

    @Column(name = "assinado_em", nullable = false)
    private Instant assinadoEm;

    public Assinatura(
            UUID documentoId,
            String provedor,
            String identificadorProvedor,
            TipoSignatario tipoSignatario,
            String nomeSignatario,
            MetodoAssinatura metodo,
            String evidencias,
            Instant assinadoEm) {
        this.id = IdUtils.novoId();
        this.documentoId = documentoId;
        this.provedor = provedor;
        this.identificadorProvedor = identificadorProvedor;
        this.tipoSignatario = tipoSignatario;
        this.nomeSignatario = nomeSignatario;
        this.metodo = metodo;
        this.evidencias = evidencias;
        this.assinadoEm = assinadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Assinatura outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return Assinatura.class.hashCode();
    }
}
