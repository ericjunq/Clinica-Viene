package br.com.clinicaviene.identidade;

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
final class RegrasDePerfil {

    private RegrasDePerfil() {}

    static boolean podeCriar(Perfil ator, Perfil novo) {
        if (novo == Perfil.DEV) {
            return false;
        }
        return ator == Perfil.DEV || ator == Perfil.MASTER;
    }

    static boolean podeVer(Perfil ator, Perfil alvo) {
        return switch (ator) {
            case DEV -> true;
            case MASTER -> alvo != Perfil.DEV;
            case ADMIN, PROFISSIONAL -> false;
        };
    }

    static boolean podeAlterarStatus(Perfil ator, Perfil alvo) {
        return switch (ator) {
            case DEV -> alvo == Perfil.ADMIN || alvo == Perfil.PROFISSIONAL;
            case MASTER -> alvo != Perfil.DEV;
            case ADMIN, PROFISSIONAL -> false;
        };
    }

    static boolean gerenciaUsuarios(Perfil ator) {
        return ator == Perfil.DEV || ator == Perfil.MASTER;
    }
}
