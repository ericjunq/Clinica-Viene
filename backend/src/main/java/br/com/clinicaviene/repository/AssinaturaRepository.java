package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Assinatura;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {}
