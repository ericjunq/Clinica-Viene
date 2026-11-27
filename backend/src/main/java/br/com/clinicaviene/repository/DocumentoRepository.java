package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Documento;
import br.com.clinicaviene.enums.TipoDocumento;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByPacienteIdOrderByCriadoEmDesc(UUID pacienteId);

    @Query("select distinct d.pacienteId from Documento d where d.tipo = :tipo and d.pacienteId in :pacientes")
    Set<UUID> pacientesComDocumento(@Param("tipo") TipoDocumento tipo, @Param("pacientes") Collection<UUID> pacientes);
}
