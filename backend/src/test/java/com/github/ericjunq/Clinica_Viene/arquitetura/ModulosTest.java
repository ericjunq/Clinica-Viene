package com.github.ericjunq.Clinica_Viene.arquitetura;

import com.github.ericjunq.Clinica_Viene.ClinicaVieneApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Verifica os limites entre os módulos do monólito modular (D-002). */
class ModulosTest {

    @Test
    void limitesDosModulosSaoRespeitados() {
        ApplicationModules.of(ClinicaVieneApplication.class).verify();
    }
}
