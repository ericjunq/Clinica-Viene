package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusUsuario;
import java.time.Instant;
import java.util.UUID;

/** Usuário na resposta da API. O CPF sai mascarado (minimização — LGPD art. 6º, III). */
public record UsuarioResponse(
        UUID id,
        String nomeCompleto,
        String cpfMascarado,
        String email,
        String telefone,
        Perfil perfil,
        StatusUsuario status,
        AreaAtuacao areaAtuacao,
        ConselhoProfissional conselho,
        String numeroRegistro,
        String ufRegistro,
        boolean trocarSenha,
        boolean doisFatoresAtivo,
        Instant criadoEm) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNomeCompleto(),
                mascararCpf(u.getCpf()),
                u.getEmail(),
                u.getTelefone(),
                u.getPerfil(),
                u.getStatus(),
                u.getAreaAtuacao(),
                u.getConselho(),
                u.getNumeroRegistro(),
                u.getUfRegistro(),
                u.isTrocarSenha(),
                u.isTotpAtivo(),
                u.getCriadoEm());
    }

    /** {@code 52601815906} → {@code ***.018.159-**}. */
    public static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
