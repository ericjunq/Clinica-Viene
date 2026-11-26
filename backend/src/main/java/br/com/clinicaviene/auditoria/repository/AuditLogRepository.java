package br.com.clinicaviene.auditoria.repository;

import br.com.clinicaviene.auditoria.entity.AuditLog;
import org.springframework.data.repository.Repository;

/** Só inclusão: de propósito não estende CrudRepository (sem delete/update). */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog entrada);
}
