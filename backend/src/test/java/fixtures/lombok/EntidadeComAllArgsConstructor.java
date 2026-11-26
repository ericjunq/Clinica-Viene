package fixtures.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** Violação proposital: {@code @AllArgsConstructor} em entidade. */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EntidadeComAllArgsConstructor {

    @Id
    private Long id;

    private String nome;
}
