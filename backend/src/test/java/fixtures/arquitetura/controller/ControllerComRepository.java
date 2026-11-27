package fixtures.arquitetura.controller;

import br.com.clinicaviene.repository.PacienteRepository;

/** Viola a regra: controller acessando repository direto, em vez de delegar ao service. */
public class ControllerComRepository {

    private final PacienteRepository repositorio;

    public ControllerComRepository(PacienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public long contar() {
        return repositorio.count();
    }
}
