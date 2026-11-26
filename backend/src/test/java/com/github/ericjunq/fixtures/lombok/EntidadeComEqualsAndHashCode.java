package com.github.ericjunq.fixtures.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;

/** Violação proposital: {@code @EqualsAndHashCode} em entidade. */
@Entity
@EqualsAndHashCode
public class EntidadeComEqualsAndHashCode {

    @Id
    private Long id;

    private String nome;
}
