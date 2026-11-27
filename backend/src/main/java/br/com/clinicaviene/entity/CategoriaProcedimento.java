package br.com.clinicaviene.entity;

import br.com.clinicaviene.enums.AreaAtuacao;
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

/**
 * Categoria de procedimento por área (D-106). O código é estável (o financeiro liga preço e repasse a ele); o nome
 * pode mudar. Nunca é apagada: desativar tira das opções novas.
 */
@Entity
@Table(name = "categoria_procedimento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoriaProcedimento {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_atuacao", nullable = false, length = 30, updatable = false)
    private AreaAtuacao areaAtuacao;

    @Column(nullable = false, length = 40, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    private boolean ativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public CategoriaProcedimento(AreaAtuacao areaAtuacao, String codigo, String nome, Instant agora) {
        this.id = IdUtils.novoId();
        this.areaAtuacao = areaAtuacao;
        this.codigo = codigo;
        this.nome = nome;
        this.ativa = true;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void atualizar(String nome, boolean ativa, Instant agora) {
        this.nome = nome;
        this.ativa = ativa;
        this.atualizadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof CategoriaProcedimento outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return CategoriaProcedimento.class.hashCode();
    }
}
