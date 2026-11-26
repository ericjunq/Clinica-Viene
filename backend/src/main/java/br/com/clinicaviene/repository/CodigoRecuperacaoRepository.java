package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.CodigoRecuperacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoRecuperacaoRepository extends JpaRepository<CodigoRecuperacao, UUID> {

    Optional<CodigoRecuperacao> findByUsuarioIdAndCodigoHashAndUsadoEmIsNull(UUID usuarioId, String codigoHash);

    List<CodigoRecuperacao> findAllByUsuarioId(UUID usuarioId);

    long countByUsuarioIdAndUsadoEmIsNull(UUID usuarioId);
}
