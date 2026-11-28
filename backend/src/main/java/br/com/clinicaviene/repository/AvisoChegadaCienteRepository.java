package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.AvisoChegadaCiente;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvisoChegadaCienteRepository extends JpaRepository<AvisoChegadaCiente, UUID> {

    Optional<AvisoChegadaCiente> findByAgendamentoId(UUID agendamentoId);

    List<AvisoChegadaCiente> findByAgendamentoIdIn(Collection<UUID> agendamentoIds);
}
