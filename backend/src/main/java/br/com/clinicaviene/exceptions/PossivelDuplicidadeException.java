package br.com.clinicaviene.exceptions;

import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Já existe paciente com o mesmo nome, data de nascimento e responsável (cadastro sem CPF — D-053). HTTP 409;
 * a recepção confere e, se for outra pessoa, envia de novo com {@code confirmarDuplicidade=true}.
 */
@Getter
public class PossivelDuplicidadeException extends RuntimeException {

    private final List<UUID> pacientesSemelhantes;

    public PossivelDuplicidadeException(List<UUID> pacientesSemelhantes) {
        super("Possível cadastro duplicado: já existe paciente com o mesmo nome, data de nascimento e responsável.");
        this.pacientesSemelhantes = List.copyOf(pacientesSemelhantes);
    }
}
