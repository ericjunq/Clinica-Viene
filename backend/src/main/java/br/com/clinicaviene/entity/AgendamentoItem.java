package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Item do agendamento: categoria + descrição (D-106). */
@Entity
@Table(name = "agendamento_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgendamentoItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, updatable = false)
    private Agendamento agendamento;

    @Column(nullable = false)
    private short ordem;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    @Column(length = 300)
    private String descricao;

    AgendamentoItem(Agendamento agendamento, int ordem, UUID categoriaId, String descricao) {
        this.id = IdUtils.novoId();
        this.agendamento = agendamento;
        this.ordem = (short) ordem;
        this.categoriaId = categoriaId;
        this.descricao = descricao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof AgendamentoItem outro && id != null && Objects.equals(id, outro.id);
    }

    @Override
    public int hashCode() {
        return AgendamentoItem.class.hashCode();
    }
}
