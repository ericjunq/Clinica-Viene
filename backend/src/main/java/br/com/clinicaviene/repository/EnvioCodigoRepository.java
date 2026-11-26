package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.EnvioCodigo;
import br.com.clinicaviene.enums.FinalidadeEnvio;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EnvioCodigoRepository extends JpaRepository<EnvioCodigo, UUID> {

    /** Envios recentes (para as regras de reenvio), do mais antigo ao mais novo. Bloqueia as linhas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EnvioCodigo> findAllByUsuarioIdAndFinalidadeAndEnviadoEmAfterOrderByEnviadoEmAsc(
            UUID usuarioId, FinalidadeEnvio finalidade, Instant desde);
}
