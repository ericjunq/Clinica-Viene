package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import java.util.Objects;
import java.util.UUID;

/**
 * Dados de um evento de auditoria. Nunca incluir dados pessoais em claro (CPF, nome, e-mail): use ids.
 *
 * @param usuarioId quem fez a ação ({@code null} quando não há usuário identificado, ex.: login com CPF
 *     inexistente)
 * @param acao o que foi feito
 * @param entidade tipo do registro afetado (ex.: {@code "Usuario"}), opcional
 * @param entidadeId id do registro afetado, opcional
 * @param justificativa motivo informado pelo usuário (ex.: acesso de emergência), opcional
 */
public record RegistroAuditoria(
        UUID usuarioId, AcaoAuditoria acao, String entidade, String entidadeId, String justificativa) {

    public RegistroAuditoria {
        Objects.requireNonNull(acao, "acao");
    }

    public static RegistroAuditoria de(UUID usuarioId, AcaoAuditoria acao) {
        return new RegistroAuditoria(usuarioId, acao, null, null, null);
    }

    public static RegistroAuditoria sobre(UUID usuarioId, AcaoAuditoria acao, String entidade, Object entidadeId) {
        return new RegistroAuditoria(
                usuarioId, acao, entidade, entidadeId == null ? null : entidadeId.toString(), null);
    }
}
