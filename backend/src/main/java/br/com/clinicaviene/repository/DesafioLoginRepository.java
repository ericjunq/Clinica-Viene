package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.DesafioLogin;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DesafioLoginRepository extends JpaRepository<DesafioLogin, UUID> {

    /** Bloqueia a linha: tentativas simultâneas com o mesmo desafio contam uma de cada vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DesafioLogin> findByTokenHash(String tokenHash);
}
