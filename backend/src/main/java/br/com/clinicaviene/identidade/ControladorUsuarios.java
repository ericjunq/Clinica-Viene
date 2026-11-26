package br.com.clinicaviene.identidade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestão de usuários: {@code /api/v1/usuarios}. Regras de acesso em {@link ServicoUsuarios}. */
@RestController
@RequestMapping("/api/v1/usuarios")
class ControladorUsuarios {

    private final ServicoUsuarios servico;
    private final UsuarioAtual usuarioAtual;

    ControladorUsuarios(ServicoUsuarios servico, UsuarioAtual usuarioAtual) {
        this.servico = servico;
        this.usuarioAtual = usuarioAtual;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResposta criar(@Valid @RequestBody NovoUsuarioRequisicao requisicao) {
        return UsuarioResposta.de(servico.criar(requisicao.paraDados(), usuarioAtual.obter()));
    }

    @GetMapping
    List<UsuarioResposta> listar() {
        return servico.listar(usuarioAtual.obter()).stream().map(UsuarioResposta::de).toList();
    }

    @GetMapping("/{id}")
    UsuarioResposta buscar(@PathVariable UUID id) {
        return UsuarioResposta.de(servico.buscar(id, usuarioAtual.obter()));
    }

    @PatchMapping("/{id}/status")
    UsuarioResposta alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusRequisicao requisicao) {
        return UsuarioResposta.de(servico.alterarStatus(id, requisicao.status(), usuarioAtual.obter()));
    }

    record NovoUsuarioRequisicao(
            @NotBlank @Size(max = 150) String nomeCompleto,
            @NotBlank String cpf,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 20) String telefone,
            @NotNull Perfil perfil,
            AreaAtuacao areaAtuacao,
            ConselhoProfissional conselho,
            @Size(max = 20) String numeroRegistro,
            @Size(max = 2) String ufRegistro,
            @NotBlank String senhaProvisoria) {

        DadosNovoUsuario paraDados() {
            return new DadosNovoUsuario(
                    nomeCompleto, cpf, email, telefone, perfil, areaAtuacao, conselho, numeroRegistro, ufRegistro,
                    senhaProvisoria);
        }

        @Override
        public String toString() {
            return "NovoUsuarioRequisicao[perfil=" + perfil + "]";
        }
    }

    record AlterarStatusRequisicao(@NotNull StatusUsuario status) {}

    /** O CPF sai mascarado (minimização — LGPD art. 6º, III). */
    record UsuarioResposta(
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

        static UsuarioResposta de(Usuario u) {
            return new UsuarioResposta(
                    u.getId(),
                    u.getNomeCompleto(),
                    mascarar(u.getCpf()),
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

        static String mascarar(String cpf) {
            if (cpf == null || cpf.length() != 11) {
                return null;
            }
            return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
        }
    }
}
