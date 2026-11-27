package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.ConviteCadastro;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ConviteCadastroRepository extends JpaRepository<ConviteCadastro, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConviteCadastro> findByTokenHash(String tokenHash);

    List<ConviteCadastro> findByPacienteIdOrderByCriadoEmDesc(UUID pacienteId);

    /** Envios por e-mail recentes (regras de reenvio — D-073), do mais antigo ao mais novo. */
    List<ConviteCadastro> findByPacienteIdAndEnviadoPorEmailEmAfterOrderByEnviadoPorEmailEmAsc(UUID pacienteId, Instant desde);
}
