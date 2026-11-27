package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.SessaoCadastro;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SessaoCadastroRepository extends JpaRepository<SessaoCadastro, UUID> {

    Optional<SessaoCadastro> findByTokenHash(String tokenHash);

    /** Códigos enviados na janela de bloqueio (regras de reenvio — D-073), do mais antigo ao mais novo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SessaoCadastro> findByConviteIdAndCodigoEnviadoEmAfterOrderByCodigoEnviadoEmAsc(UUID conviteId, Instant desde);
}
