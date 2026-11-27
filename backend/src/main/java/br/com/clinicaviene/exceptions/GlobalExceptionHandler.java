package br.com.clinicaviene.exceptions;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros da API no formato Problem Details (RFC 9457). Mensagens de erro nunca contêm dados pessoais e
 * erros inesperados não expõem detalhes internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraDeNegocio(RegraNegocioException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({
        CredenciaisInvalidasException.class,
        DesafioInvalidoException.class,
        CodigoInvalidoException.class,
        SessaoInvalidaException.class
    })
    ProblemDetail naoAutenticado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(PossivelDuplicidadeException.class)
    ProblemDetail possivelDuplicidade(PossivelDuplicidadeException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setProperty("pacientesSemelhantes", e.getPacientesSemelhantes());
        return problema;
    }

    @ExceptionHandler(RenovacaoConcorrenteException.class)
    ProblemDetail renovacaoConcorrente(RenovacaoConcorrenteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    ResponseEntity<ProblemDetail> muitasTentativas(MuitasTentativasException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSegundosParaTentarDeNovo()))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        // Só o tipo da exceção no log: a mensagem pode conter dados pessoais.
        log.error("Erro inesperado: {}", e.getClass().getName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno.");
    }

    /** Erros de validação ({@code @Valid}) com a lista de campos inválidos em {@code erros}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail corpo = ex.getBody();
        corpo.setDetail("Dados inválidos.");
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro.getField(),
                        "mensagem", erro.getDefaultMessage() == null ? "inválido" : erro.getDefaultMessage()))
                .toList();
        corpo.setProperty("erros", erros);
        return handleExceptionInternal(ex, corpo, headers, status, request);
    }
}
