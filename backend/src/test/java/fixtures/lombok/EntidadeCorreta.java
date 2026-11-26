package fixtures.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entidade no padrão do projeto (D-051): Lombok só onde permitido, equals/hashCode à mão pelo id. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntidadeCorreta {

    @Id
    private Long id;

    private String nome;

    public EntidadeCorreta(String nome) {
        this.nome = nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntidadeCorreta outra)) {
            return false;
        }
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        // Constante por classe: estável antes e depois de o id ser gerado.
        return Objects.hash(EntidadeCorreta.class);
    }
}
