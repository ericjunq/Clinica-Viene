package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.StatusVersao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModeloAnamneseRepository extends JpaRepository<ModeloAnamnese, UUID> {

    List<ModeloAnamnese> findAllByOrderByAreaAtuacaoAscVersaoDesc();

    Optional<ModeloAnamnese> findFirstByAreaAtuacaoAndStatusOrderByVersaoDesc(AreaAtuacao area, StatusVersao status);

    @Query("select coalesce(max(m.versao), 0) from ModeloAnamnese m where m.areaAtuacao = :area")
    int ultimaVersao(@Param("area") AreaAtuacao area);
}
