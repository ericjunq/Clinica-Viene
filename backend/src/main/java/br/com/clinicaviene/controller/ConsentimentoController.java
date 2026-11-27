package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.ConsentimentoResponse;
import br.com.clinicaviene.dto.RevogarConsentimentoRequest;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.service.ConsentimentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

/** Consentimentos do paciente (recepção/MASTER): {@code /api/v1/pacientes/{id}/consentimentos}. */
@RestController
@RequestMapping("/api/v1/pacientes/{pacienteId}/consentimentos")
@RequiredArgsConstructor
public class ConsentimentoController {

    private final ConsentimentoService consentimentoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<ConsentimentoResponse> situacao(@PathVariable UUID pacienteId) {
        return consentimentoService.situacao(pacienteId, usuarioAutenticadoProvider.obter());
    }

    /** O paciente pediu, na recepção, para deixar de receber/usar algo opcional. */
    @PostMapping("/{tipo}/revogar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogar(
            @PathVariable UUID pacienteId,
            @PathVariable TipoTermo tipo,
            @Valid @RequestBody(required = false) RevogarConsentimentoRequest request,
            HttpServletRequest http) {
        consentimentoService.revogar(
                pacienteId, tipo, request == null ? null : request.justificativa(), http.getRemoteAddr(),
                usuarioAutenticadoProvider.obter());
    }
}
