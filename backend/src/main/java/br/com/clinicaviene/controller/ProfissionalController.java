package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.EncerrarHorarioFixoRequest;
import br.com.clinicaviene.dto.ExcecaoAgendaResponse;
import br.com.clinicaviene.dto.HorarioFixoResponse;
import br.com.clinicaviene.dto.HorariosProfissionalResponse;
import br.com.clinicaviene.dto.IntervaloDisponivelDto;
import br.com.clinicaviene.dto.NovaExcecaoAgendaRequest;
import br.com.clinicaviene.dto.NovoHorarioFixoRequest;
import br.com.clinicaviene.dto.ProfissionalResumoResponse;
import br.com.clinicaviene.service.DisponibilidadeService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Profissionais na agenda e os horários deles: {@code /api/v1/profissionais}. */
@RestController
@RequestMapping("/api/v1/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

    private final DisponibilidadeService disponibilidadeService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<ProfissionalResumoResponse> listar() {
        return disponibilidadeService.profissionais(usuarioAutenticadoProvider.obter()).stream()
                .map(ProfissionalResumoResponse::from)
                .toList();
    }

    @GetMapping("/{id}/horarios")
    public HorariosProfissionalResponse horarios(@PathVariable UUID id) {
        return disponibilidadeService.horarios(id, usuarioAutenticadoProvider.obter());
    }

    @PostMapping("/{id}/horarios-fixos")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioFixoResponse criarFixo(@PathVariable UUID id, @Valid @RequestBody NovoHorarioFixoRequest request) {
        return HorarioFixoResponse.from(disponibilidadeService.criarFixo(id, request, usuarioAutenticadoProvider.obter()));
    }

    @PostMapping("/{id}/horarios-fixos/{horarioId}/encerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void encerrarFixo(
            @PathVariable UUID id, @PathVariable UUID horarioId, @Valid @RequestBody EncerrarHorarioFixoRequest request) {
        disponibilidadeService.encerrarFixo(id, horarioId, request.ultimoDia(), usuarioAutenticadoProvider.obter());
    }

    @PostMapping("/{id}/excecoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ExcecaoAgendaResponse criarExcecao(@PathVariable UUID id, @Valid @RequestBody NovaExcecaoAgendaRequest request) {
        return ExcecaoAgendaResponse.from(disponibilidadeService.criarExcecao(id, request, usuarioAutenticadoProvider.obter()));
    }

    @PostMapping("/{id}/excecoes/{excecaoId}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarExcecao(@PathVariable UUID id, @PathVariable UUID excecaoId) {
        disponibilidadeService.cancelarExcecao(id, excecaoId, usuarioAutenticadoProvider.obter());
    }

    /** Trechos em que o profissional atende, já com extras e ausências (para o calendário). */
    @GetMapping("/{id}/disponibilidade")
    public List<IntervaloDisponivelDto> disponibilidade(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return disponibilidadeService.disponiveis(id, de, ate, usuarioAutenticadoProvider.obter());
    }
}
