package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.FichaAnamnese;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaAnamneseRepository extends JpaRepository<FichaAnamnese, UUID> {}
