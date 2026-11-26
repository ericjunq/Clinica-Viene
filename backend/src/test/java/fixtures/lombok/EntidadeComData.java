package fixtures.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/** Violação proposital: {@code @Data} em entidade. */
@Entity
@Data
public class EntidadeComData {

    @Id
    private Long id;

    private String nome;
}
