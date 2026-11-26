package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.Perfil;
import java.util.Objects;
import java.util.UUID;

/** Quem está fazendo a requisição. Obtido do token de acesso (1D). */
public record UsuarioAutenticado(UUID id, Perfil perfil) {

    public UsuarioAutenticado {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(perfil, "perfil");
    }

    public boolean eh(Perfil outro) {
        return perfil == outro;
    }
}
