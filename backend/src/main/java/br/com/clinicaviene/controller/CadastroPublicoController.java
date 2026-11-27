package br.com.clinicaviene.controller;

import br.com.clinicaviene.dto.CodigoCadastroEnviadoResponse;
import br.com.clinicaviene.dto.ConfirmarCodigoCadastroRequest;
import br.com.clinicaviene.dto.ConviteAbertoResponse;
import br.com.clinicaviene.dto.FichaCadastroRequest;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.dto.RevisaoCadastroResponse;
import br.com.clinicaviene.dto.SessaoCadastroResponse;
import br.com.clinicaviene.service.CadastroPublicoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fluxo público do cadastro do paciente (sem login — D-091, D-100). O token do convite vai no cabeçalho
 * {@value #CONVITE} e o da sessão de preenchimento em {@value #SESSAO} (fora da URL, para não parar em logs).
 */
@RestController
@RequestMapping("/api/v1/publico/cadastro")
@RequiredArgsConstructor
public class CadastroPublicoController {

    public static final String CONVITE = "X-Convite-Cadastro";
    public static final String SESSAO = "X-Sessao-Cadastro";

    private final CadastroPublicoService cadastroPublicoService;

    @GetMapping
    public ConviteAbertoResponse abrir(@RequestHeader(name = CONVITE, required = false) String convite, HttpServletRequest http) {
        return cadastroPublicoService.abrir(convite, http.getRemoteAddr());
    }

    @PostMapping("/codigo")
    public CodigoCadastroEnviadoResponse enviarCodigo(
            @RequestHeader(name = CONVITE, required = false) String convite, HttpServletRequest http) {
        return cadastroPublicoService.enviarCodigo(convite, http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
    }

    @PostMapping("/codigo/confirmar")
    public SessaoCadastroResponse confirmarCodigo(
            @RequestHeader(name = CONVITE, required = false) String convite,
            @Valid @RequestBody ConfirmarCodigoCadastroRequest request,
            HttpServletRequest http) {
        return cadastroPublicoService.confirmarCodigo(
                convite, request.codigo(), http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
    }

    @PostMapping("/quiosque")
    public SessaoCadastroResponse iniciarQuiosque(
            @RequestHeader(name = CONVITE, required = false) String convite, HttpServletRequest http) {
        return cadastroPublicoService.iniciarQuiosque(convite, http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
    }

    @GetMapping("/formulario")
    public FormularioCadastroResponse formulario(
            @RequestHeader(name = CONVITE, required = false) String convite,
            @RequestHeader(name = SESSAO, required = false) String sessao) {
        return cadastroPublicoService.formulario(convite, sessao);
    }

    @PostMapping("/revisao")
    public RevisaoCadastroResponse revisar(
            @RequestHeader(name = CONVITE, required = false) String convite,
            @RequestHeader(name = SESSAO, required = false) String sessao,
            @Valid @RequestBody FichaCadastroRequest ficha) {
        return cadastroPublicoService.revisar(convite, sessao, ficha);
    }
}
