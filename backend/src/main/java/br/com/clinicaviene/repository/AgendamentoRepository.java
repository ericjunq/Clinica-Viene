package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Agendamento;
import br.com.clinicaviene.enums.StatusAgendamento;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    /** Agendamentos que tocam [de, ate) (os filtros por profissional/consultório ficam no service). */
    @EntityGraph(attributePaths = "itens")
    @Query("""
            select distinct a from Agendamento a
            where a.inicio < :ate and a.fim > :de
            order by a.inicio""")
    List<Agendamento> doPeriodo(@Param("de") Instant de, @Param("ate") Instant ate);

    /** Agendamentos ativos (não cancelados) que se sobrepõem ao período, do profissional ou do consultório. */
    @Query("""
            select a from Agendamento a
            where a.status <> br.com.clinicaviene.enums.StatusAgendamento.CANCELADO
              and a.inicio < :fim and a.fim > :inicio
              and (a.profissionalId = :profissional or a.consultorioId = :consultorio)
              and a.id <> :ignorar""")
    List<Agendamento> conflitantes(
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            @Param("profissional") UUID profissionalId,
            @Param("consultorio") UUID consultorioId,
            @Param("ignorar") UUID ignorar);

    /** Consultas futuras ainda em aberto do profissional (para conferir mudanças de horário). */
    List<Agendamento> findByProfissionalIdAndStatusInAndInicioGreaterThanEqual(
            UUID profissionalId, Collection<StatusAgendamento> status, Instant aPartirDe);
}
