package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.CategoriaProcedimento;
import br.com.clinicaviene.enums.AreaAtuacao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaProcedimentoRepository extends JpaRepository<CategoriaProcedimento, UUID> {

    List<CategoriaProcedimento> findAllByOrderByAreaAtuacaoAscNomeAsc();

    List<CategoriaProcedimento> findAllByAreaAtuacaoOrderByNomeAsc(AreaAtuacao area);

    boolean existsByAreaAtuacaoAndCodigo(AreaAtuacao area, String codigo);

    boolean existsByAreaAtuacaoAndNomeIgnoreCase(AreaAtuacao area, String nome);

    boolean existsByAreaAtuacaoAndNomeIgnoreCaseAndIdNot(AreaAtuacao area, String nome, UUID id);
}
