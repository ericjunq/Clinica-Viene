package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.AlterarPerfilRequest;
import br.com.clinicaviene.dto.AlterarStatusRequest;
import br.com.clinicaviene.dto.AtualizarUsuarioRequest;
import br.com.clinicaviene.dto.NovoUsuarioRequest;
import br.com.clinicaviene.dto.TrocarSenhaRequest;
import br.com.clinicaviene.dto.UsuarioResponse;
import br.com.clinicaviene.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestão de usuários: {@code /api/v1/usuarios}. As regras de acesso ficam no {@link UsuarioService}. */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody NovoUsuarioRequest request) {
        return UsuarioResponse.from(usuarioService.criar(request.toDto(), usuarioAutenticadoProvider.obter()));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar(usuarioAutenticadoProvider.obter()).stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return UsuarioResponse.from(usuarioService.buscar(id, usuarioAutenticadoProvider.obter()));
    }

    /** Troca da própria senha (permitida mesmo com troca obrigatória pendente). */
    @PostMapping("/eu/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarPropriaSenha(@Valid @RequestBody TrocarSenhaRequest request) {
        usuarioService.trocarPropriaSenha(usuarioAutenticadoProvider.obter(), request.senhaAtual(), request.novaSenha());
    }

    /** Edita nome, e-mail, telefone e registro profissional (CPF e perfil não mudam aqui — D-086). */
    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioRequest request) {
        return UsuarioResponse.from(usuarioService.atualizarDados(
                id,
                request.nomeCompleto(),
                request.email(),
                request.telefone(),
                request.registro(),
                usuarioAutenticadoProvider.obter()));
    }

    /** Muda o perfil (com o registro profissional, se for PROFISSIONAL). Derruba as sessões do usuário. */
    @PatchMapping("/{id}/perfil")
    public UsuarioResponse alterarPerfil(@PathVariable UUID id, @Valid @RequestBody AlterarPerfilRequest request) {
        return UsuarioResponse.from(usuarioService.alterarPerfil(
                id, request.perfil(), request.registro(), usuarioAutenticadoProvider.obter()));
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusRequest request) {
        return UsuarioResponse.from(
                usuarioService.alterarStatus(id, request.status(), usuarioAutenticadoProvider.obter()));
    }
}
