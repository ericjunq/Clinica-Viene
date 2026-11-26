package com.github.ericjunq.fixtures.lombok;

import lombok.Value;

/** Violação proposital: {@code @Value} (deveria ser record). */
@Value
public class ClasseComValue {

    String nome;
}
