package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.exceptions.RegraNegocioException;
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

/** Versão de um termo (D-089). Rascunho editável; publicado é imutável (o banco também garante). */
@Entity
@Table(name = "termo_versao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermoVersao {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private TipoTermo tipo;

    @Column(nullable = false, updatable = false)
    private int versao;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusVersao status;

    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "publicado_por")
    private UUID publicadoPor;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    public TermoVersao(TipoTermo tipo, int versao, String titulo, String texto, UUID criadoPor, Instant agora) {
        this.id = IdUtils.novoId();
        this.tipo = tipo;
        this.versao = versao;
        this.titulo = titulo;
        this.texto = texto;
        this.status = StatusVersao.RASCUNHO;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public boolean publicado() {
        return status == StatusVersao.PUBLICADO;
    }

    public void editar(String titulo, String texto) {
        exigirRascunho();
        this.titulo = titulo;
        this.texto = texto;
    }

    public void publicar(UUID por, Instant agora) {
        exigirRascunho();
        this.status = StatusVersao.PUBLICADO;
        this.publicadoPor = por;
        this.publicadoEm = agora;
    }

    private void exigirRascunho() {
        if (publicado()) {
            throw new RegraNegocioException("Versão publicada não pode ser alterada. Crie uma nova versão.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TermoVersao outra)) {
            return false;
        }
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return TermoVersao.class.hashCode();
    }
}
