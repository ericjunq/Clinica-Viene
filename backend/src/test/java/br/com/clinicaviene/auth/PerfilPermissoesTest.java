package br.com.clinicaviene.auth;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.clinicaviene.enums.Perfil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Matriz completa de perfil × perfil (CONTEXTO §2, D-067, D-068). */
public class PerfilPermissoesTest {

    @ParameterizedTest(name = "{0} cria {1}: {2}")
    @CsvSource({
        "DEV, DEV, false",
        "DEV, MASTER, true",
        "DEV, ADMIN, true",
        "DEV, PROFISSIONAL, true",
        "MASTER, DEV, false",
        "MASTER, MASTER, true",
        "MASTER, ADMIN, true",
        "MASTER, PROFISSIONAL, true",
        "ADMIN, DEV, false",
        "ADMIN, MASTER, false",
        "ADMIN, ADMIN, false",
        "ADMIN, PROFISSIONAL, false",
        "PROFISSIONAL, DEV, false",
        "PROFISSIONAL, MASTER, false",
        "PROFISSIONAL, ADMIN, false",
        "PROFISSIONAL, PROFISSIONAL, false"
    })
    void podeCriar(Perfil ator, Perfil novo, boolean esperado) {
        assertThat(PerfilPermissoes.podeCriar(ator, novo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} vê {1}: {2}")
    @CsvSource({
        "DEV, DEV, true",
        "DEV, MASTER, true",
        "DEV, ADMIN, true",
        "DEV, PROFISSIONAL, true",
        "MASTER, DEV, false",
        "MASTER, MASTER, true",
        "MASTER, ADMIN, true",
        "MASTER, PROFISSIONAL, true",
        "ADMIN, ADMIN, false",
        "PROFISSIONAL, PROFISSIONAL, false"
    })
    void podeVer(Perfil ator, Perfil alvo, boolean esperado) {
        assertThat(PerfilPermissoes.podeVer(ator, alvo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} ativa/desativa {1}: {2}")
    @CsvSource({
        "DEV, DEV, false",
        "DEV, MASTER, false",
        "DEV, ADMIN, true",
        "DEV, PROFISSIONAL, true",
        "MASTER, DEV, false",
        "MASTER, MASTER, true",
        "MASTER, ADMIN, true",
        "MASTER, PROFISSIONAL, true",
        "ADMIN, PROFISSIONAL, false",
        "PROFISSIONAL, ADMIN, false"
    })
    void podeAlterarStatus(Perfil ator, Perfil alvo, boolean esperado) {
        assertThat(PerfilPermissoes.podeAlterarStatus(ator, alvo)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"DEV, true", "MASTER, true", "ADMIN, false", "PROFISSIONAL, false"})
    void gerenciaUsuarios(Perfil ator, boolean esperado) {
        assertThat(PerfilPermissoes.gerenciaUsuarios(ator)).isEqualTo(esperado);
    }
}
