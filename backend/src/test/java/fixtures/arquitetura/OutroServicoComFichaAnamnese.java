package fixtures.arquitetura;

import br.com.clinicaviene.repository.FichaAnamneseRepository;

/** Viola a regra: lê a anamnese sem passar pelo FichaAnamneseService (sem permissão nem auditoria). */
public class OutroServicoComFichaAnamnese {

    private final FichaAnamneseRepository repositorio;

    public OutroServicoComFichaAnamnese(FichaAnamneseRepository repositorio) {
        this.repositorio = repositorio;
    }

    public long contar() {
        return repositorio.count();
    }
}
