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

/** Consultório (sala) da clínica (CONTEXTO §7). Nunca é apagado: desativar tira da agenda. */
@Entity
@Table(name = "consultorio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultorio {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Consultorio(String nome, Instant agora) {
        this.id = IdUtils.novoId();
        this.nome = nome;
        this.ativo = true;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void atualizar(String nome, boolean ativo, Instant agora) {
        this.nome = nome;
        this.ativo = ativo;
        this.atualizadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Consultorio outro && id != null && Objects.equals(id, outro.id);
    }

    @Override
    public int hashCode() {
        return Consultorio.class.hashCode();
    }
}
