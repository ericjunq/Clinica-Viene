package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.TokenRenovacao;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRenovacaoRepository extends JpaRepository<TokenRenovacao, UUID> {

    /** Bloqueia a linha: duas renovações simultâneas do mesmo token são processadas uma de cada vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenRenovacao> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TokenRenovacao t set t.revogadoEm = :agora where t.cadeiaId = :cadeia and t.revogadoEm is null")
    int revogarCadeia(@Param("cadeia") UUID cadeiaId, @Param("agora") Instant agora);

    @Modifying
    @Query("update TokenRenovacao t set t.revogadoEm = :agora where t.usuarioId = :usuario and t.revogadoEm is null")
    int revogarDoUsuario(@Param("usuario") UUID usuarioId, @Param("agora") Instant agora);
}
