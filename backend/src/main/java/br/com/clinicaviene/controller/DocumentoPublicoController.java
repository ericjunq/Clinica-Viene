package br.com.clinicaviene.controller;

import br.com.clinicaviene.dto.BaixarDocumentoRequest;
import br.com.clinicaviene.dto.DocumentoBaixadoDto;
import br.com.clinicaviene.service.EntregaDocumentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Download público de documentos pelo link enviado ao paciente (D-094). O token do link vai no cabeçalho
 * {@value #ENTREGA}; o CPF, no corpo.
 */
@RestController
@RequestMapping("/api/v1/publico/documentos")
@RequiredArgsConstructor
public class DocumentoPublicoController {

    public static final String ENTREGA = "X-Entrega-Documento";

    private final EntregaDocumentoService entregaDocumentoService;

    @PostMapping("/baixar")
    public ResponseEntity<byte[]> baixar(
            @RequestHeader(name = ENTREGA, required = false) String token,
            @Valid @RequestBody BaixarDocumentoRequest request,
            HttpServletRequest http) {
        DocumentoBaixadoDto documento = entregaDocumentoService.baixar(token, request.cpf(), http.getRemoteAddr());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(documento.nomeArquivo()).build().toString())
                .body(documento.conteudo());
    }
}
