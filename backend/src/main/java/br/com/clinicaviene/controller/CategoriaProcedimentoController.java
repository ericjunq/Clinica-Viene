package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.CategoriaProcedimentoResponse;
import br.com.clinicaviene.dto.EditarCategoriaProcedimentoRequest;
import br.com.clinicaviene.dto.NovaCategoriaProcedimentoRequest;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.service.CategoriaProcedimentoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Categorias de procedimento por área: {@code /api/v1/categorias-procedimento}. */
@RestController
@RequestMapping("/api/v1/categorias-procedimento")
@RequiredArgsConstructor
public class CategoriaProcedimentoController {

    private final CategoriaProcedimentoService categoriaService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<CategoriaProcedimentoResponse> listar(@RequestParam(required = false) AreaAtuacao area) {
        return categoriaService.listar(area, usuarioAutenticadoProvider.obter()).stream()
                .map(CategoriaProcedimentoResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaProcedimentoResponse criar(@Valid @RequestBody NovaCategoriaProcedimentoRequest request) {
        return CategoriaProcedimentoResponse.from(categoriaService.criar(
                request.areaAtuacao(), request.codigo(), request.nome(), usuarioAutenticadoProvider.obter()));
    }

    @PutMapping("/{id}")
    public CategoriaProcedimentoResponse atualizar(
            @PathVariable UUID id, @Valid @RequestBody EditarCategoriaProcedimentoRequest request) {
        return CategoriaProcedimentoResponse.from(categoriaService.atualizar(
                id, request.nome(), request.estaAtiva(), usuarioAutenticadoProvider.obter()));
    }

    /** Cria as categorias de exemplo da odontologia que ainda não existem. */
    @PostMapping("/exemplos/odontologia")
    public List<CategoriaProcedimentoResponse> carregarExemploOdontologia() {
        return categoriaService.carregarExemploOdontologia(usuarioAutenticadoProvider.obter()).stream()
                .map(CategoriaProcedimentoResponse::from)
                .toList();
    }
}
