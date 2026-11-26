package br.com.clinicaviene.controller;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.auth.UsuarioAutenticadoProvider;
import br.com.clinicaviene.dto.CadastroDoisFatoresResponse;
import br.com.clinicaviene.dto.ConfirmarDoisFatoresRequest;
import br.com.clinicaviene.dto.DesafioRequest;
import br.com.clinicaviene.dto.LoginRequest;
import br.com.clinicaviene.dto.LoginResponse;
import br.com.clinicaviene.dto.SessaoComCodigosDto;
import br.com.clinicaviene.dto.SessaoDto;
import br.com.clinicaviene.dto.SessaoResponse;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.dto.UsuarioResponse;
import br.com.clinicaviene.dto.VerificarDoisFatoresRequest;
import br.com.clinicaviene.service.DoisFatoresService;
import br.com.clinicaviene.service.LoginService;
import br.com.clinicaviene.service.SessaoService;
import br.com.clinicaviene.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login em 2 passos e sessão (D-071, D-072): {@code /api/v1/auth}.
 *
 * <ol>
 *   <li>{@code POST /login}: CPF + senha → desafio e próxima etapa;
 *   <li>primeiro acesso: {@code POST /2fa/cadastro} (QR code) → {@code POST /2fa/confirmar} (primeiro código);
 *   <li>acessos seguintes: {@code POST /2fa/verificar} (código do app ou de recuperação);
 *   <li>{@code POST /renovar} e {@code POST /logout} usam o cookie HttpOnly de renovação.
 * </ol>
 *
 * <p>O token de renovação só trafega no cookie ({@code HttpOnly; SameSite=Strict; Path=/api/v1/auth}); o
 * token de acesso vai no corpo e o frontend o mantém só em memória.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE_RENOVACAO = "cv_renovacao";
    private static final String CAMINHO_COOKIE = "/api/v1/auth";

    private final LoginService loginService;
    private final DoisFatoresService doisFatoresService;
    private final SessaoService sessaoService;
    private final UsuarioService usuarioService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final AuthProperties authProperties;
    private final Clock clock;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return loginService.login(request.cpf(), request.senha(), http.getRemoteAddr());
    }

    @PostMapping("/2fa/cadastro")
    public CadastroDoisFatoresResponse iniciarCadastroDoisFatores(@Valid @RequestBody DesafioRequest request) {
        return doisFatoresService.iniciarCadastro(request.desafio());
    }

    @PostMapping("/2fa/confirmar")
    public ResponseEntity<SessaoResponse> confirmarCadastroDoisFatores(
            @Valid @RequestBody ConfirmarDoisFatoresRequest request) {
        SessaoComCodigosDto resultado = doisFatoresService.confirmarCadastro(request.desafio(), request.codigo());
        return comCookie(resultado.sessao(), resultado.codigosRecuperacao());
    }

    @PostMapping("/2fa/verificar")
    public ResponseEntity<SessaoResponse> verificarDoisFatores(@Valid @RequestBody VerificarDoisFatoresRequest request) {
        return comCookie(doisFatoresService.verificar(request.desafio(), request.metodo(), request.codigo()), null);
    }

    @PostMapping("/renovar")
    public ResponseEntity<SessaoResponse> renovar(
            @CookieValue(name = COOKIE_RENOVACAO, required = false) String tokenRenovacao) {
        return comCookie(sessaoService.renovar(tokenRenovacao), null);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_RENOVACAO, required = false) String tokenRenovacao) {
        sessaoService.encerrar(tokenRenovacao);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/eu")
    public UsuarioResponse eu() {
        UsuarioAutenticado usuario = usuarioAutenticadoProvider.obter();
        return UsuarioResponse.from(usuarioService.buscar(usuario.id(), usuario));
    }

    private ResponseEntity<SessaoResponse> comCookie(SessaoDto sessao, List<String> codigosRecuperacao) {
        Duration validade = Duration.between(Instant.now(clock), sessao.sessaoExpiraEm());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(sessao.tokenRenovacao(), validade).toString())
                .body(SessaoResponse.from(sessao, codigosRecuperacao));
    }

    private ResponseCookie cookie(String valor, Duration validade) {
        return ResponseCookie.from(COOKIE_RENOVACAO, valor)
                .httpOnly(true)
                .secure(authProperties.cookieSeguro())
                .sameSite("Strict")
                .path(CAMINHO_COOKIE)
                .maxAge(validade.isNegative() ? Duration.ZERO : validade)
                .build();
    }
}
