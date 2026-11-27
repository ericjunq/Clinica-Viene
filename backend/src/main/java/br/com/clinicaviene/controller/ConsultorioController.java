package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.ConsultorioRequest;
import br.com.clinicaviene.dto.ConsultorioResponse;
import br.com.clinicaviene.service.ConsultorioService;
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

/** Consultórios: {@code /api/v1/consultorios}. */
@RestController
@RequestMapping("/api/v1/consultorios")
@RequiredArgsConstructor
public class ConsultorioController {

    private final ConsultorioService consultorioService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<ConsultorioResponse> listar() {
        return consultorioService.listar(usuarioAutenticadoProvider.obter()).stream()
                .map(ConsultorioResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultorioResponse criar(@Valid @RequestBody ConsultorioRequest request) {
        return ConsultorioResponse.from(consultorioService.criar(request.nome(), usuarioAutenticadoProvider.obter()));
    }

    @PutMapping("/{id}")
    public ConsultorioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ConsultorioRequest request) {
        return ConsultorioResponse.from(consultorioService.atualizar(
                id, request.nome(), request.estaAtivo(), usuarioAutenticadoProvider.obter()));
    }
}
