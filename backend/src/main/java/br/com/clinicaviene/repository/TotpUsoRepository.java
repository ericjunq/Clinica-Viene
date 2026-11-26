package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.TotpUso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TotpUsoRepository extends JpaRepository<TotpUso, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TotpUso> findWithLockByUsuarioId(UUID usuarioId);
}
