package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.AgendamentoResponse;
import br.com.clinicaviene.dto.AlterarItensAgendamentoRequest;
import br.com.clinicaviene.dto.AlterarStatusAgendamentoRequest;
import br.com.clinicaviene.dto.CancelarAgendamentoRequest;
import br.com.clinicaviene.dto.NovoAgendamentoRequest;
import br.com.clinicaviene.dto.RemarcarAgendamentoRequest;
import br.com.clinicaviene.service.AgendamentoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Agenda: {@code /api/v1/agendamentos}. Regras e permissões no {@link AgendamentoService}. */
@RestController
@RequestMapping("/api/v1/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<AgendamentoResponse> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) UUID profissionalId,
            @RequestParam(required = false) UUID consultorioId) {
        return agendamentoService.listar(de, ate, profissionalId, consultorioId, usuarioAutenticadoProvider.obter());
    }

    @GetMapping("/{id}")
    public AgendamentoResponse buscar(@PathVariable UUID id) {
        return agendamentoService.buscar(id, usuarioAutenticadoProvider.obter());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criar(@Valid @RequestBody NovoAgendamentoRequest request) {
        return agendamentoService.criar(request, usuarioAutenticadoProvider.obter());
    }

    @PutMapping("/{id}/itens")
    public AgendamentoResponse alterarItens(@PathVariable UUID id, @Valid @RequestBody AlterarItensAgendamentoRequest request) {
        return agendamentoService.alterarItens(id, request.itens(), request.observacao(), usuarioAutenticadoProvider.obter());
    }

    /** Confirmar, check-in (PACIENTE_CHEGOU), início/fim do atendimento ou falta. */
    @PostMapping("/{id}/status")
    public AgendamentoResponse mudarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusAgendamentoRequest request) {
        return agendamentoService.mudarStatus(id, request.status(), usuarioAutenticadoProvider.obter());
    }

    @PostMapping("/{id}/cancelar")
    public AgendamentoResponse cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarAgendamentoRequest request) {
        return agendamentoService.cancelar(id, request.motivo(), usuarioAutenticadoProvider.obter());
    }

    @PostMapping("/{id}/remarcar")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse remarcar(@PathVariable UUID id, @Valid @RequestBody RemarcarAgendamentoRequest request) {
        return agendamentoService.remarcar(id, request, usuarioAutenticadoProvider.obter());
    }
}
