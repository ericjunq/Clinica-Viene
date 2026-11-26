package com.github.ericjunq.fixtures.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.ToString;

/** Violação proposital: {@code @ToString} em entidade. */
@Entity
@ToString
public class EntidadeComToString {

    @Id
    private Long id;

    private String nome;
}
