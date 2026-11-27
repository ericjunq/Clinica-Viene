package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.ResponsavelLegal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsavelLegalRepository extends JpaRepository<ResponsavelLegal, UUID> {

    Optional<ResponsavelLegal> findByPacienteId(UUID pacienteId);
}
