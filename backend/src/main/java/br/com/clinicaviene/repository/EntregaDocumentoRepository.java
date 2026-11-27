package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.EntregaDocumento;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EntregaDocumentoRepository extends JpaRepository<EntregaDocumento, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EntregaDocumento> findByTokenHash(String tokenHash);
}
