package br.com.clinicaviene.repository;

import br.com.clinicaviene.entity.AuditLog;
import org.springframework.data.repository.Repository;

/** Só inclusão: de propósito não estende CrudRepository (sem delete/update). */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog entrada);
}
