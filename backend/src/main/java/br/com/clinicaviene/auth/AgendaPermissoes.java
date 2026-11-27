package br.com.clinicaviene.auth;

import br.com.clinicaviene.enums.Perfil;

/**
 * Quem faz o quê na agenda (CONTEXTO §2, D-106). DEV não acessa a agenda (tem dados de pacientes).
 *
 * <ul>
 *   <li>Recepção (ADMIN) e MASTER: horários dos profissionais e agendamentos.
 *   <li>MASTER: consultórios e categorias de procedimento (configuração).
 *   <li>PROFISSIONAL: vê a própria agenda e os próprios horários.
 * </ul>
 */
public final class AgendaPermissoes {

    private AgendaPermissoes() {}

    public static boolean gerenciaAgenda(Perfil perfil) {
        return perfil == Perfil.ADMIN || perfil == Perfil.MASTER;
    }

    public static boolean configuraAgenda(Perfil perfil) {
        return perfil == Perfil.MASTER;
    }

    public static boolean usaAgenda(Perfil perfil) {
        return perfil == Perfil.ADMIN || perfil == Perfil.MASTER || perfil == Perfil.PROFISSIONAL;
    }
}
