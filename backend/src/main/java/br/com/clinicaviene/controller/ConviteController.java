package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.ConviteCriadoResponse;
import br.com.clinicaviene.dto.ConviteResponse;
import br.com.clinicaviene.dto.NovoConviteRequest;
import br.com.clinicaviene.service.ConviteService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Convites de cadastro (recepção/MASTER): {@code /api/v1/pacientes/{id}/convites}. */
@RestController
@RequestMapping("/api/v1/pacientes/{pacienteId}/convites")
@RequiredArgsConstructor
public class ConviteController {

    private final ConviteService conviteService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConviteCriadoResponse criar(@PathVariable UUID pacienteId, @Valid @RequestBody NovoConviteRequest request) {
        return conviteService.criar(
                pacienteId, request.modo(), request.deveEnviarEmail(), request.documentoFoiConferido(),
                usuarioAutenticadoProvider.obter());
    }

    @GetMapping
    public List<ConviteResponse> listar(@PathVariable UUID pacienteId) {
        Instant agora = conviteService.agoraClinica();
        return conviteService.listar(pacienteId, usuarioAutenticadoProvider.obter()).stream()
                .map(c -> ConviteResponse.from(c, agora))
                .toList();
    }

    @PostMapping("/{conviteId}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID pacienteId, @PathVariable UUID conviteId) {
        conviteService.cancelar(pacienteId, conviteId, usuarioAutenticadoProvider.obter());
    }
}
