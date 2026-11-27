package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoriaProcedimentoServiceTest {

    @Test
    void geraCodigoEstavelAPartirDoNome() {
        assertThat(CategoriaProcedimentoService.codigoDoNome("Clínica geral")).isEqualTo("CLINICA_GERAL");
        assertThat(CategoriaProcedimentoService.codigoDoNome("Endodontia (canal)")).isEqualTo("ENDODONTIA_CANAL");
        assertThat(CategoriaProcedimentoService.codigoDoNome("3D escaneamento")).isEqualTo("C_3D_ESCANEAMENTO");
        assertThat(CategoriaProcedimentoService.codigoDoNome("x".repeat(60))).hasSize(40);
    }
}
