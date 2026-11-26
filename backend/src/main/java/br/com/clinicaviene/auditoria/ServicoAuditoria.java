package br.com.clinicaviene.auditoria;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registro de auditoria (D-006, D-076). API pública do módulo {@code auditoria}.
 *
 * <p>O IP é obtido da requisição HTTP atual, quando houver (atrás do nginx, via cabeçalhos de proxy —
 * {@code server.forward-headers-strategy}).
 */
@Service
public class ServicoAuditoria {

    private final RepositorioAuditoria repositorio;
    private final Clock relogio;

    ServicoAuditoria(RepositorioAuditoria repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * Registra dentro da transação atual: se a operação for desfeita (rollback), o registro também é. Use
     * para ações que só aconteceram se a operação foi concluída (ex.: usuário criado).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(RegistroAuditoria registro) {
        gravar(registro);
    }

    /**
     * Registra em transação própria: fica gravado mesmo que a operação em andamento falhe. Use para eventos
     * de segurança (ex.: login com senha errada, tentativa negada).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIndependente(RegistroAuditoria registro) {
        gravar(registro);
    }

    private void gravar(RegistroAuditoria registro) {
        repositorio.save(new EntradaAuditoria(registro, ipDaRequisicaoAtual(), Instant.now(relogio)));
    }

    private static String ipDaRequisicaoAtual() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            HttpServletRequest requisicao = servlet.getRequest();
            return requisicao.getRemoteAddr();
        }
        return null;
    }
}
