package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;

/**
 * Dados para criar um usuário, já normalizados (CPF só com dígitos, e-mail em minúsculas).
 *
 * @param senhaProvisoria senha inicial definida por quem cria; troca obrigatória no primeiro login
 */
public record NovoUsuarioDto(
        String nomeCompleto,
        String cpf,
        String email,
        String telefone,
        Perfil perfil,
        AreaAtuacao areaAtuacao,
        ConselhoProfissional conselho,
        String numeroRegistro,
        String ufRegistro,
        String senhaProvisoria) {

    @Override
    public String toString() {
        // Nunca expor CPF ou senha em logs.
        return "NovoUsuarioDto[perfil=" + perfil + "]";
    }
}
