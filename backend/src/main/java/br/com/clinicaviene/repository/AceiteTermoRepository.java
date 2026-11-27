package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.AceiteTermo;
import br.com.clinicaviene.enums.TipoTermo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Só inclusão e leitura (sem delete/update). */
public interface AceiteTermoRepository extends Repository<AceiteTermo, UUID> {

    AceiteTermo save(AceiteTermo aceite);

    Optional<AceiteTermo> findFirstByPacienteIdAndTipoOrderByRegistradoEmDesc(UUID pacienteId, TipoTermo tipo);

    List<AceiteTermo> findByPacienteIdOrderByRegistradoEmDesc(UUID pacienteId);
}
