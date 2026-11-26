package br.com.clinicaviene.identidade;

import br.com.clinicaviene.auditoria.AcaoAuditoria;
import br.com.clinicaviene.auditoria.RegistroAuditoria;
import br.com.clinicaviene.auditoria.ServicoAuditoria;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sessões (D-071): JWT de acesso curto + token de renovação opaco, rotacionado a cada uso.
 *
 * <ul>
 *   <li>A sessão tem fim absoluto ({@code duracaoSessao} a partir do login); renovar não estende.
 *   <li>Reusar um token já trocado: se for logo após a troca (corrida entre abas), responde
 *       {@link RenovacaoConcorrente} sem revogar; depois disso, trata como roubo e revoga a cadeia.
 *   <li>Usuário inativo não renova.
 * </ul>
 */
@Service
class ServicoSessoes {

    private static final int BYTES_TOKEN = 32;

    private final RepositorioTokensRenovacao tokens;
    private final RepositorioUsuarios usuarios;
    private final EmissorTokenAcesso emissor;
    private final ServicoAuditoria auditoria;
    private final PropriedadesAutenticacao propriedades;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    ServicoSessoes(
            RepositorioTokensRenovacao tokens,
            RepositorioUsuarios usuarios,
            EmissorTokenAcesso emissor,
            ServicoAuditoria auditoria,
            PropriedadesAutenticacao propriedades,
            Clock relogio) {
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.emissor = emissor;
        this.auditoria = auditoria;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    /** Abre uma sessão nova (chamado depois do 2FA concluído). */
    @Transactional
    Sessao iniciar(Usuario usuario) {
        Instant agora = agora();
        return emitir(usuario, UUID.randomUUID(), agora.plus(propriedades.duracaoSessao()), agora);
    }

    @Transactional(noRollbackFor = SessaoInvalida.class)
    Sessao renovar(String valorToken) {
        Instant agora = agora();
        TokenRenovacao atual = buscar(valorToken).orElseThrow(SessaoInvalida::new);
        if (atual.revogado() || atual.expirado(agora)) {
            throw new SessaoInvalida();
        }
        if (atual.usado()) {
            if (atual.getUsadoEm().plus(propriedades.toleranciaRenovacaoConcorrente()).isAfter(agora)) {
                throw new RenovacaoConcorrente();
            }
            tokens.revogarCadeia(atual.getCadeiaId(), agora);
            auditoria.registrarIndependente(RegistroAuditoria.sobre(
                    atual.getUsuarioId(), AcaoAuditoria.SESSAO_REUSO_DETECTADO, "Sessao", atual.getCadeiaId()));
            throw new SessaoInvalida();
        }
        Usuario usuario = usuarios.findById(atual.getUsuarioId())
                .filter(Usuario::ativo)
                .orElseThrow(SessaoInvalida::new);
        atual.marcarUsado(agora);
        return emitir(usuario, atual.getCadeiaId(), atual.getExpiraEm(), agora);
    }

    /** Logout: revoga a cadeia do token informado (se existir). */
    @Transactional
    void encerrar(String valorToken) {
        buscar(valorToken).ifPresent(token -> {
            tokens.revogarCadeia(token.getCadeiaId(), agora());
            auditoria.registrar(RegistroAuditoria.sobre(token.getUsuarioId(), AcaoAuditoria.LOGOUT, "Sessao", token.getCadeiaId()));
        });
    }

    /** Derruba todas as sessões do usuário (ex.: desativado — D-068). */
    @Transactional
    void revogarTodas(UUID usuarioId) {
        tokens.revogarDoUsuario(usuarioId, agora());
    }

    private Optional<TokenRenovacao> buscar(String valorToken) {
        if (valorToken == null || valorToken.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash(valorToken));
    }

    private Sessao emitir(Usuario usuario, UUID cadeia, Instant fimDaSessao, Instant agora) {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String valorRenovacao = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new TokenRenovacao(usuario.getId(), cadeia, hash(valorRenovacao), agora, fimDaSessao));
        EmissorTokenAcesso.TokenAcesso acesso = emissor.emitir(usuario);
        return new Sessao(acesso.valor(), acesso.expiraEm(), valorRenovacao, fimDaSessao, usuario);
    }

    static String hash(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Instant agora() {
        return Instant.now(relogio);
    }

    /**
     * Sessão emitida. {@code tokenRenovacao} vai só no cookie HttpOnly, nunca no corpo da resposta.
     */
    record Sessao(
            String tokenAcesso, Instant acessoExpiraEm, String tokenRenovacao, Instant sessaoExpiraEm, Usuario usuario) {

        @Override
        public String toString() {
            return "Sessao[usuario=" + usuario.getId() + "]";
        }
    }

    /** Token de renovação inválido, expirado, revogado ou reusado: o usuário precisa fazer login de novo. */
    static final class SessaoInvalida extends RuntimeException {
        SessaoInvalida() {
            super("Sessão inválida ou expirada.");
        }
    }

    /** Token trocado há instantes por outra aba: o cliente deve tentar de novo com o cookie atualizado. */
    static final class RenovacaoConcorrente extends RuntimeException {
        RenovacaoConcorrente() {
            super("Renovação concorrente; tente novamente.");
        }
    }
}
