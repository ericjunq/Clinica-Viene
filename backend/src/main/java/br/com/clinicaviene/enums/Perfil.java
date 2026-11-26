package br.com.clinicaviene.enums;

/** Perfis de acesso (CONTEXTO §2). Persistido como texto. */
public enum Perfil {
    /** Desenvolvedor (o autor). Independente do MASTER; criado só pelo inicializador (D-067). */
    DEV,
    /** Dono(a) da clínica. */
    MASTER,
    /** Recepção. */
    ADMIN,
    /** Profissional de saúde, com área de atuação e registro profissional (D-069). */
    PROFISSIONAL
}
