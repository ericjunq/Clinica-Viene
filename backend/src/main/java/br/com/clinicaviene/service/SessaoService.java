package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.SessaoDto;
import br.com.clinicaviene.dto.TokenAcessoDto;
import br.com.clinicaviene.entity.TokenRenovacao;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.exceptions.RenovacaoConcorrenteException;
import br.com.clinicaviene.exceptions.SessaoInvalidaException;
import br.com.clinicaviene.repository.TokenRenovacaoRepository;
import br.com.clinicaviene.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sessões (D-071): JWT de acesso curto + token de renovação opaco, rotacionado a cada uso.
 *
 * <ul>
 *   <li>A sessão tem fim absoluto ({@code duracaoSessao} a partir do login); renovar não estende.
 *   <li>Reusar um token já trocado: se for logo após a troca (corrida entre abas), responde
 *       {@link RenovacaoConcorrenteException} sem revogar; depois disso, trata como roubo e revoga a cadeia.
 *   <li>Usuário inativo não renova.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SessaoService {

    private static final int BYTES_TOKEN = 32;

    private final TokenRenovacaoRepository tokens;
    private final UsuarioRepository usuarios;
    private final TokenAcessoService emissor;
    private final AuditoriaService auditoria;
    private final AuthProperties propriedades;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    /** Abre uma sessão nova (chamado depois do 2FA concluído). */
    @Transactional
    public SessaoDto iniciar(Usuario usuario) {
        Instant agora = agora();
        return emitir(usuario, UUID.randomUUID(), agora.plus(propriedades.duracaoSessao()), agora);
    }

    @Transactional(noRollbackFor = SessaoInvalidaException.class)
    public SessaoDto renovar(String valorToken) {
        Instant agora = agora();
        TokenRenovacao atual = buscar(valorToken).orElseThrow(SessaoInvalidaException::new);
        if (atual.revogado() || atual.expirado(agora)) {
            throw new SessaoInvalidaException();
        }
        if (atual.usado()) {
            if (atual.getUsadoEm().plus(propriedades.toleranciaRenovacaoConcorrente()).isAfter(agora)) {
                throw new RenovacaoConcorrenteException();
            }
            tokens.revogarCadeia(atual.getCadeiaId(), agora);
            auditoria.registrarIndependente(RegistroAuditoria.sobre(
                    atual.getUsuarioId(), AcaoAuditoria.SESSAO_REUSO_DETECTADO, "Sessao", atual.getCadeiaId()));
            throw new SessaoInvalidaException();
        }
        Usuario usuario = usuarios.findById(atual.getUsuarioId())
                .filter(Usuario::ativo)
                .orElseThrow(SessaoInvalidaException::new);
        atual.marcarUsado(agora);
        return emitir(usuario, atual.getCadeiaId(), atual.getExpiraEm(), agora);
    }

    /** Logout: revoga a cadeia do token informado (se existir). */
    @Transactional
    public void encerrar(String valorToken) {
        buscar(valorToken).ifPresent(token -> {
            tokens.revogarCadeia(token.getCadeiaId(), agora());
            auditoria.registrar(RegistroAuditoria.sobre(token.getUsuarioId(), AcaoAuditoria.LOGOUT, "Sessao", token.getCadeiaId()));
        });
    }

    /** Derruba todas as sessões do usuário (ex.: desativado — D-068). */
    @Transactional
    public void revogarTodas(UUID usuarioId) {
        tokens.revogarDoUsuario(usuarioId, agora());
    }

    private Optional<TokenRenovacao> buscar(String valorToken) {
        if (valorToken == null || valorToken.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash(valorToken));
    }

    private SessaoDto emitir(Usuario usuario, UUID cadeia, Instant fimDaSessao, Instant agora) {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String valorRenovacao = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new TokenRenovacao(usuario.getId(), cadeia, hash(valorRenovacao), agora, fimDaSessao));
        TokenAcessoDto acesso = emissor.emitir(usuario);
        return new SessaoDto(acesso.valor(), acesso.expiraEm(), valorRenovacao, fimDaSessao, usuario);
    }

    public static String hash(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Truncado em microssegundos: a precisão do PostgreSQL (o valor em memória fica igual ao gravado). */
    private Instant agora() {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
