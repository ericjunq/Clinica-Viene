package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.ExcecaoAgenda;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExcecaoAgendaRepository extends JpaRepository<ExcecaoAgenda, UUID> {

    /** Exceções ativas que tocam o período [de, ate). */
    @Query("""
            select e from ExcecaoAgenda e
            where e.profissionalId = :profissional
              and e.canceladaEm is null
              and e.inicio < :ate and e.fim > :de
            order by e.inicio""")
    List<ExcecaoAgenda> ativasEntre(
            @Param("profissional") UUID profissionalId, @Param("de") Instant de, @Param("ate") Instant ate);
}
