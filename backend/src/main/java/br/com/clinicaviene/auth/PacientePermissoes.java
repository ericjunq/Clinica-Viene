package br.com.clinicaviene.auth;

import br.com.clinicaviene.enums.Perfil;

/**
 * Quem acessa o cadastro de pacientes (CONTEXTO §2): recepção (ADMIN) e MASTER. DEV não acessa dados de
 * pacientes (D-003). Profissional verá os pacientes vinculados a ele na Fase 4. Conteúdo clínico (anamnese)
 * tem regra própria (D-088).
 */
public final class PacientePermissoes {

    private PacientePermissoes() {}

    public static boolean gerenciaCadastro(Perfil perfil) {
        return perfil == Perfil.ADMIN || perfil == Perfil.MASTER;
    }
}
