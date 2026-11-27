package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.AtualizarContatoPacienteRequest;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.PacienteResponse;
import br.com.clinicaviene.dto.PacienteResumoResponse;
import br.com.clinicaviene.dto.PacienteVersaoResponse;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.service.PacienteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro de pacientes: {@code /api/v1/pacientes}. Regras de acesso no {@link PacienteService}. */
@RestController
@RequestMapping("/api/v1/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse preCadastrar(@Valid @RequestBody NovoPacienteRequest request) {
        return resposta(pacienteService.preCadastrar(request, usuarioAutenticadoProvider.obter()));
    }

    @GetMapping
    public List<PacienteResumoResponse> pesquisar(@RequestParam(name = "busca", defaultValue = "") String busca) {
        return pacienteService.pesquisar(busca, usuarioAutenticadoProvider.obter()).stream()
                .map(PacienteResumoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PacienteResponse buscar(@PathVariable UUID id) {
        UsuarioAutenticado ator = usuarioAutenticadoProvider.obter();
        return resposta(pacienteService.buscar(id, ator));
    }

    @PatchMapping("/{id}/contato")
    public PacienteResponse atualizarContato(
            @PathVariable UUID id, @Valid @RequestBody AtualizarContatoPacienteRequest request) {
        return resposta(pacienteService.atualizarContato(
                id, request.email(), request.telefone(), request.endereco(), usuarioAutenticadoProvider.obter()));
    }

    @GetMapping("/{id}/historico")
    public List<PacienteVersaoResponse> historico(@PathVariable UUID id) {
        return pacienteService.historico(id, usuarioAutenticadoProvider.obter()).stream()
                .map(PacienteVersaoResponse::from)
                .toList();
    }

    private PacienteResponse resposta(Paciente paciente) {
        return PacienteResponse.from(
                paciente, pacienteService.responsavelDe(paciente.getId()).orElse(null), pacienteService.hoje());
    }
}
