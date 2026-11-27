package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.DisponibilidadeProfissional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisponibilidadeProfissionalRepository extends JpaRepository<DisponibilidadeProfissional, UUID> {

    /** Horários fixos que valem em algum dia de [de, ate]. */
    @Query("""
            select d from DisponibilidadeProfissional d
            where d.profissionalId = :profissional
              and d.vigenciaInicio <= :ate
              and (d.vigenciaFim is null or d.vigenciaFim >= :de)
            order by d.diaSemana, d.horaInicio""")
    List<DisponibilidadeProfissional> vigentesEntre(
            @Param("profissional") UUID profissionalId, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Horários fixos que ainda valem a partir de {@code data} (para a tela de configuração). */
    @Query("""
            select d from DisponibilidadeProfissional d
            where d.profissionalId = :profissional
              and (d.vigenciaFim is null or d.vigenciaFim >= :data)
            order by d.diaSemana, d.horaInicio""")
    List<DisponibilidadeProfissional> vigentesAPartirDe(
            @Param("profissional") UUID profissionalId, @Param("data") LocalDate data);
}
