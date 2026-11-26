package br.com.clinicaviene.auditoria;

import org.springframework.data.repository.Repository;

/** Só inclusão: de propósito não estende CrudRepository (sem delete/update). */
interface RepositorioAuditoria extends Repository<EntradaAuditoria, Long> {

    EntradaAuditoria save(EntradaAuditoria entrada);
}
