package br.com.clinicaviene.arquitetura;

import br.com.clinicaviene.ClinicaVieneApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Verifica os limites entre os módulos do monólito modular (D-002). */
class ModulosTest {

    @Test
    void limitesDosModulosSaoRespeitados() {
        ApplicationModules.of(ClinicaVieneApplication.class).verify();
    }
}
