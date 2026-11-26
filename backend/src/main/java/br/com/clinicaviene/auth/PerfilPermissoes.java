package br.com.clinicaviene.auth;

import br.com.clinicaviene.enums.Perfil;

/**
 * Quem pode fazer o quê com usuários (CONTEXTO §2, D-067, D-068). Regras puras, testadas isoladamente.
 *
 * <ul>
 *   <li>Nenhum perfil cria DEV (só o inicializador).
 *   <li>DEV e MASTER criam MASTER, ADMIN e PROFISSIONAL.
 *   <li>MASTER não vê nem gerencia DEV; DEV não desativa MASTER.
 *   <li>ADMIN e PROFISSIONAL não gerenciam usuários.
 * </ul>
 */
public final class PerfilPermissoes {

    private PerfilPermissoes() {}

    public static boolean podeCriar(Perfil ator, Perfil novo) {
        if (novo == Perfil.DEV) {
            return false;
        }
        return ator == Perfil.DEV || ator == Perfil.MASTER;
    }

    public static boolean podeVer(Perfil ator, Perfil alvo) {
        return switch (ator) {
            case DEV -> true;
            case MASTER -> alvo != Perfil.DEV;
            case ADMIN, PROFISSIONAL -> false;
        };
    }

    public static boolean podeAlterarStatus(Perfil ator, Perfil alvo) {
        return switch (ator) {
            case DEV -> alvo == Perfil.ADMIN || alvo == Perfil.PROFISSIONAL;
            case MASTER -> alvo != Perfil.DEV;
            case ADMIN, PROFISSIONAL -> false;
        };
    }

    public static boolean gerenciaUsuarios(Perfil ator) {
        return ator == Perfil.DEV || ator == Perfil.MASTER;
    }
}
