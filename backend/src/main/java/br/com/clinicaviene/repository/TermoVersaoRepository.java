package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.enums.TipoTermo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermoVersaoRepository extends JpaRepository<TermoVersao, UUID> {

    List<TermoVersao> findAllByOrderByTipoAscVersaoDesc();

    /** Versão vigente do tipo: a publicada mais recente. */
    Optional<TermoVersao> findFirstByTipoAndStatusOrderByVersaoDesc(TipoTermo tipo, StatusVersao status);

    @Query("select coalesce(max(t.versao), 0) from TermoVersao t where t.tipo = :tipo")
    int ultimaVersao(@Param("tipo") TipoTermo tipo);

    boolean existsByTipoAndStatus(TipoTermo tipo, StatusVersao status);
}
