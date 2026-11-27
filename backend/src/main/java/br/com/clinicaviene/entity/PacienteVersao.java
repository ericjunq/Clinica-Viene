package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.OrigemAlteracao;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Retrato do cadastro a cada alteração (D-092). Dados cifrados; somente inclusão (o banco rejeita UPDATE e
 * DELETE). {@code camposAlterados} guarda só os nomes dos campos, nunca valores.
 */
@Entity
@Immutable
@Table(name = "paciente_versao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PacienteVersao {

    @Id
    private UUID id;

    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;

    @Getter(AccessLevel.NONE)
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(nullable = false, updatable = false)
    private String dados;

    @Column(name = "campos_alterados", nullable = false, length = 300, updatable = false)
    private String camposAlterados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private OrigemAlteracao origem;

    @Column(name = "alterado_por", updatable = false)
    private UUID alteradoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public PacienteVersao(
            UUID pacienteId, String dadosJson, String camposAlterados, OrigemAlteracao origem, UUID alteradoPor, Instant agora) {
        this.id = IdUtils.novoId();
        this.pacienteId = pacienteId;
        this.dados = dadosJson;
        this.camposAlterados = camposAlterados;
        this.origem = origem;
        this.alteradoPor = alteradoPor;
        this.criadoEm = agora;
    }

    /** Retrato completo (JSON). Só para uso interno do service; não vai na API. */
    public String dadosJson() {
        return dados;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PacienteVersao outra)) {
            return false;
        }
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return PacienteVersao.class.hashCode();
    }
}
