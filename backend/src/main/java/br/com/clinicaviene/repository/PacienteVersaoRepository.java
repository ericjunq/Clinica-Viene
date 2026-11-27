package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.PacienteVersao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Só inclusão e leitura: de propósito não estende CrudRepository (sem delete/update). */
public interface PacienteVersaoRepository extends Repository<PacienteVersao, UUID> {

    PacienteVersao save(PacienteVersao versao);

    List<PacienteVersao> findByPacienteIdOrderByCriadoEmDesc(UUID pacienteId);
}
