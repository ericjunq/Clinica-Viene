package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Assinatura;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    List<Assinatura> findByDocumentoIdIn(Collection<UUID> documentoIds);

    Optional<Assinatura> findFirstByDocumentoId(UUID documentoId);
}
