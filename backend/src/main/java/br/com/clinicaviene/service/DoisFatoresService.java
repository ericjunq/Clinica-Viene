package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.dto.CadastroDoisFatoresResponse;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.dto.SessaoComCodigosDto;
import br.com.clinicaviene.dto.SessaoDto;
import br.com.clinicaviene.entity.CodigoRecuperacao;
import br.com.clinicaviene.entity.DesafioLogin;
import br.com.clinicaviene.entity.TotpUso;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.MetodoDoisFatores;
import br.com.clinicaviene.exceptions.CodigoInvalidoException;
import br.com.clinicaviene.exceptions.DesafioInvalidoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.CodigoRecuperacaoRepository;
import br.com.clinicaviene.repository.DesafioLoginRepository;
import br.com.clinicaviene.repository.TotpUsoRepository;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CodigoRecuperacaoUtils;
import br.com.clinicaviene.util.QrCodeUtils;
import br.com.clinicaviene.util.TokenUtils;
import br.com.clinicaviene.util.TotpUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Segundo passo do login (D-072): cadastro do app autenticador (TOTP) no primeiro acesso, verificação do
 * código nos acessos seguintes e códigos de recuperação. Só aqui a sessão é aberta.
 *
 * <p>Código errado conta uma tentativa no desafio; esgotadas as tentativas, o desafio morre e é preciso
 * informar a senha de novo. Um código TOTP só vale uma vez (anti-replay).
 */
@Service
@RequiredArgsConstructor
public class DoisFatoresService {

    /** Aceita o código do passo anterior e do seguinte (relógio do celular um pouco fora). */
    private static final int JANELA_TOTP = 1;

    private static final int QUANTIDADE_CODIGOS_RECUPERACAO = 10;
    private static final String EMISSOR_TOTP = "ClinicaViene";

    private final DesafioLoginRepository desafioLoginRepository;
    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacaoRepository codigoRecuperacaoRepository;
    private final TotpUsoRepository totpUsoRepository;
    private final SessaoService sessaoService;
    private final AuditoriaService auditoriaService;
    private final AuthProperties authProperties;
    private final Clock clock;

    /** Primeiro acesso: gera o segredo do app autenticador e o QR code. */
    @Transactional(noRollbackFor = DesafioInvalidoException.class)
    public CadastroDoisFatoresResponse iniciarCadastro(String tokenDesafio) {
        DesafioLogin desafio = desafioValido(tokenDesafio);
        Usuario usuario = usuarioAtivo(desafio);
        if (usuario.isTotpAtivo()) {
            throw new RegraNegocioException("O 2FA já está cadastrado.");
        }
        String segredo = TotpUtils.gerarSegredo();
        desafio.definirSegredoPendente(segredo);
        String uri = TotpUtils.uriOtpAuth(EMISSOR_TOTP, usuario.getEmail(), segredo);
        return new CadastroDoisFatoresResponse(segredo, uri, QrCodeUtils.pngDataUri(uri));
    }

    /** Primeiro acesso: confere o primeiro código do app, ativa o 2FA, gera os códigos de recuperação e entra. */
    @Transactional(noRollbackFor = {CodigoInvalidoException.class, DesafioInvalidoException.class})
    public SessaoComCodigosDto confirmarCadastro(String tokenDesafio, String codigo) {
        DesafioLogin desafio = desafioValido(tokenDesafio);
        Usuario usuario = usuarioAtivo(desafio);
        if (usuario.isTotpAtivo()) {
            throw new RegraNegocioException("O 2FA já está cadastrado.");
        }
        String segredo = desafio.segredoPendente();
        if (segredo == null) {
            throw new RegraNegocioException("Gere o QR code do 2FA antes de confirmar.");
        }
        OptionalLong passo = TotpUtils.verificar(segredo, codigo, passoAtual(), JANELA_TOTP);
        if (passo.isEmpty() || !registrarUsoTotp(usuario, passo.getAsLong())) {
            throw falha(desafio, usuario);
        }
        Instant agora = agora();
        usuario.ativarTotp(segredo, agora);
        List<String> codigos = gerarCodigosRecuperacao(usuario, agora);
        desafio.concluir(agora);
        auditoriaService.registrar(RegistroAuditoria.sobre(
                usuario.getId(), AcaoAuditoria.DOIS_FATORES_CADASTRADO, "Usuario", usuario.getId()));
        return new SessaoComCodigosDto(sessaoService.iniciar(usuario), codigos);
    }

    /** Acessos seguintes: confere o código do app (TOTP) ou um código de recuperação, e entra. */
    @Transactional(noRollbackFor = {CodigoInvalidoException.class, DesafioInvalidoException.class})
    public SessaoDto verificar(String tokenDesafio, MetodoDoisFatores metodo, String codigo) {
        DesafioLogin desafio = desafioValido(tokenDesafio);
        Usuario usuario = usuarioAtivo(desafio);
        if (!usuario.isTotpAtivo()) {
            throw new RegraNegocioException("Cadastre o 2FA antes de entrar.");
        }
        Instant agora = agora();
        switch (metodo) {
            case TOTP -> {
                OptionalLong passo = TotpUtils.verificar(usuario.totpSegredo(), codigo, passoAtual(), JANELA_TOTP);
                if (passo.isEmpty() || !registrarUsoTotp(usuario, passo.getAsLong())) {
                    throw falha(desafio, usuario);
                }
            }
            case RECUPERACAO -> {
                String hash = TokenUtils.sha256(CodigoRecuperacaoUtils.normalizar(codigo));
                CodigoRecuperacao recuperacao = codigoRecuperacaoRepository
                        .findByUsuarioIdAndCodigoHashAndUsadoEmIsNull(usuario.getId(), hash)
                        .orElseThrow(() -> falha(desafio, usuario));
                recuperacao.marcarUsado(agora);
                auditoriaService.registrar(RegistroAuditoria.sobre(
                        usuario.getId(), AcaoAuditoria.CODIGO_RECUPERACAO_USADO, "Usuario", usuario.getId()));
            }
        }
        desafio.concluir(agora);
        auditoriaService.registrar(RegistroAuditoria.de(usuario.getId(), AcaoAuditoria.DOIS_FATORES_OK));
        return sessaoService.iniciar(usuario);
    }

    private DesafioLogin desafioValido(String tokenDesafio) {
        if (tokenDesafio == null || tokenDesafio.isBlank()) {
            throw new DesafioInvalidoException();
        }
        DesafioLogin desafio = desafioLoginRepository
                .findByTokenHash(TokenUtils.sha256(tokenDesafio))
                .orElseThrow(DesafioInvalidoException::new);
        if (!desafio.valido(agora(), authProperties.tentativasDesafio())) {
            throw new DesafioInvalidoException();
        }
        return desafio;
    }

    private Usuario usuarioAtivo(DesafioLogin desafio) {
        return usuarioRepository
                .findById(desafio.getUsuarioId())
                .filter(Usuario::ativo)
                .orElseThrow(DesafioInvalidoException::new);
    }

    /** Conta a tentativa errada no desafio e audita (em transação própria). */
    private CodigoInvalidoException falha(DesafioLogin desafio, Usuario usuario) {
        desafio.registrarFalha();
        auditoriaService.registrarIndependente(RegistroAuditoria.de(usuario.getId(), AcaoAuditoria.DOIS_FATORES_FALHA));
        return new CodigoInvalidoException();
    }

    /** Aceita o passo só se for posterior ao último usado: o mesmo código não entra duas vezes. */
    private boolean registrarUsoTotp(Usuario usuario, long passo) {
        Optional<TotpUso> uso = totpUsoRepository.findWithLockByUsuarioId(usuario.getId());
        if (uso.isEmpty()) {
            totpUsoRepository.save(new TotpUso(usuario.getId(), passo));
            return true;
        }
        if (passo <= uso.get().getUltimoPasso()) {
            return false;
        }
        uso.get().registrar(passo);
        return true;
    }

    private List<String> gerarCodigosRecuperacao(Usuario usuario, Instant agora) {
        codigoRecuperacaoRepository.deleteAll(codigoRecuperacaoRepository.findAllByUsuarioId(usuario.getId()));
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < QUANTIDADE_CODIGOS_RECUPERACAO; i++) {
            String codigo = CodigoRecuperacaoUtils.gerar();
            codigos.add(codigo);
            codigoRecuperacaoRepository.save(new CodigoRecuperacao(
                    usuario.getId(), TokenUtils.sha256(CodigoRecuperacaoUtils.normalizar(codigo)), agora));
        }
        return codigos;
    }

    private long passoAtual() {
        return TotpUtils.passo(Instant.now(clock));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
