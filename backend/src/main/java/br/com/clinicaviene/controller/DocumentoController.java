package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.DocumentoResponse;
import br.com.clinicaviene.dto.EntregaDocumentoCriadaResponse;
import br.com.clinicaviene.dto.NovaEntregaDocumentoRequest;
import br.com.clinicaviene.service.DocumentoService;
import br.com.clinicaviene.service.EntregaDocumentoService;
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

/** Documentos assinados do paciente (recepção/MASTER): {@code /api/v1/pacientes/{id}/documentos}. */
@RestController
@RequestMapping("/api/v1/pacientes/{pacienteId}/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final EntregaDocumentoService entregaDocumentoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @GetMapping
    public List<DocumentoResponse> listar(@PathVariable UUID pacienteId) {
        return documentoService.listar(pacienteId, usuarioAutenticadoProvider.obter());
    }

    /** Novo link de download para o paciente (mostrado como QR na tela e, se pedido, enviado por e-mail). */
    @PostMapping("/{documentoId}/entregas")
    @ResponseStatus(HttpStatus.CREATED)
    public EntregaDocumentoCriadaResponse gerarEntrega(
            @PathVariable UUID pacienteId,
            @PathVariable UUID documentoId,
            @RequestBody(required = false) NovaEntregaDocumentoRequest request) {
        return entregaDocumentoService.gerarPelaRecepcao(
                pacienteId, documentoId, request != null && request.deveEnviarEmail(), usuarioAutenticadoProvider.obter());
    }
}
