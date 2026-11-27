package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.EditarModeloAnamneseRequest;
import br.com.clinicaviene.dto.ModeloAnamneseResponse;
import br.com.clinicaviene.dto.NovoModeloAnamneseRequest;
import br.com.clinicaviene.service.ModeloAnamneseService;
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

/** Configuração dos modelos de anamnese (MASTER): {@code /api/v1/modelos-anamnese}. */
@RestController
@RequestMapping("/api/v1/modelos-anamnese")
@RequiredArgsConstructor
public class ModeloAnamneseController {

    private final ModeloAnamneseService modeloAnamneseService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<ModeloAnamneseResponse> listar() {
        return modeloAnamneseService.listar(usuarioAutenticadoProvider.obter()).stream()
                .map(modeloAnamneseService::resposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ModeloAnamneseResponse buscar(@PathVariable UUID id) {
        return modeloAnamneseService.resposta(modeloAnamneseService.buscar(id, usuarioAutenticadoProvider.obter()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloAnamneseResponse criar(@Valid @RequestBody NovoModeloAnamneseRequest request) {
        return modeloAnamneseService.resposta(modeloAnamneseService.criarRascunho(
                request.areaAtuacao(), request.titulo(), request.perguntas(), usuarioAutenticadoProvider.obter()));
    }

    @PutMapping("/{id}")
    public ModeloAnamneseResponse editar(@PathVariable UUID id, @Valid @RequestBody EditarModeloAnamneseRequest request) {
        return modeloAnamneseService.resposta(modeloAnamneseService.editar(
                id, request.titulo(), request.perguntas(), usuarioAutenticadoProvider.obter()));
    }

    @PostMapping("/{id}/publicar")
    public ModeloAnamneseResponse publicar(@PathVariable UUID id) {
        return modeloAnamneseService.resposta(modeloAnamneseService.publicar(id, usuarioAutenticadoProvider.obter()));
    }

    @PostMapping("/{id}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloAnamneseResponse duplicar(@PathVariable UUID id) {
        return modeloAnamneseService.resposta(modeloAnamneseService.duplicar(id, usuarioAutenticadoProvider.obter()));
    }

    /** Cria o rascunho com o exemplo completo de ficha odontológica (revisar com a dentista antes de publicar). */
    @PostMapping("/exemplos/odontologia")
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloAnamneseResponse carregarExemploOdontologia() {
        return modeloAnamneseService.resposta(
                modeloAnamneseService.carregarExemploOdontologia(usuarioAutenticadoProvider.obter()));
    }
}
