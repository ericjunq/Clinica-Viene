package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.StatusVersao;
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

/**
 * Modelo de anamnese de uma área de atuação, versionado (D-089). Perguntas em JSON. Publicado é imutável
 * (o banco também garante): as fichas assinadas apontam para a versão usada.
 */
@Entity
@Table(name = "modelo_anamnese")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModeloAnamnese {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_atuacao", nullable = false, length = 30, updatable = false)
    private AreaAtuacao areaAtuacao;

    @Column(nullable = false, updatable = false)
    private int versao;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String perguntas;

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

    public ModeloAnamnese(
            AreaAtuacao areaAtuacao, int versao, String titulo, String perguntasJson, UUID criadoPor, Instant agora) {
        this.id = IdUtils.novoId();
        this.areaAtuacao = areaAtuacao;
        this.versao = versao;
        this.titulo = titulo;
        this.perguntas = perguntasJson;
        this.status = StatusVersao.RASCUNHO;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public String perguntasJson() {
        return perguntas;
    }

    public boolean publicado() {
        return status == StatusVersao.PUBLICADO;
    }

    public void editar(String titulo, String perguntasJson) {
        exigirRascunho();
        this.titulo = titulo;
        this.perguntas = perguntasJson;
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
        if (!(o instanceof ModeloAnamnese outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return ModeloAnamnese.class.hashCode();
    }
}
