package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.EditarTermoRequest;
import br.com.clinicaviene.dto.NovoTermoRequest;
import br.com.clinicaviene.dto.TermoResponse;
import br.com.clinicaviene.service.TermoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Configuração dos termos LGPD (MASTER): {@code /api/v1/termos}. */
@RestController
@RequestMapping("/api/v1/termos")
@RequiredArgsConstructor
public class TermoController {

    private final TermoService termoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<TermoResponse> listar() {
        return termoService.listar(usuarioAutenticadoProvider.obter()).stream().map(TermoResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermoResponse criar(@Valid @RequestBody NovoTermoRequest request) {
        return TermoResponse.from(termoService.criarRascunho(
                request.tipo(), request.titulo(), request.texto(), usuarioAutenticadoProvider.obter()));
    }

    @PutMapping("/{id}")
    public TermoResponse editar(@PathVariable UUID id, @Valid @RequestBody EditarTermoRequest request) {
        return TermoResponse.from(
                termoService.editar(id, request.titulo(), request.texto(), usuarioAutenticadoProvider.obter()));
    }

    @PostMapping("/{id}/publicar")
    public TermoResponse publicar(@PathVariable UUID id) {
        return TermoResponse.from(termoService.publicar(id, usuarioAutenticadoProvider.obter()));
    }

    /** Cria rascunhos com os textos de exemplo (revisar com advogado antes de publicar). */
    @PostMapping("/exemplos")
    public List<TermoResponse> carregarExemplos() {
        return termoService.carregarExemplos(usuarioAutenticadoProvider.obter()).stream()
                .map(TermoResponse::from)
                .toList();
    }
}
