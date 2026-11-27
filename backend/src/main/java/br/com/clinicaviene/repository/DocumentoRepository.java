package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.Documento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByPacienteIdOrderByCriadoEmDesc(UUID pacienteId);
}
