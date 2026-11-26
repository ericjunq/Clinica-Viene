package br.com.clinicaviene.service;

import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.FinalidadeEnvio;
import br.com.clinicaviene.exceptions.CodigoInvalidoException;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import br.com.clinicaviene.util.CpfUtils;
import br.com.clinicaviene.validation.PoliticaSenhaValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperação de senha por e-mail (1G): o usuário informa o CPF, recebe um código de 6 dígitos no e-mail
 * cadastrado e define uma senha nova. O 2FA continua exigido no próximo login.
 *
 * <p>A solicitação responde sempre igual, exista ou não o CPF (não revela quem tem cadastro); por isso,
 * quando o reenvio está em espera, a solicitação é ignorada em silêncio em vez de responder 429.
 */
@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {

    private final UsuarioRepository usuarioRepository;
    private final CpfHasher cpfHasher;
    private final EnvioCodigoService envioCodigoService;
    private final SessaoService sessaoService;
    private final LimiteTentativasService limiteTentativasService;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public void solicitar(String cpf, String ip) {
        limiteTentativasService.verificarLogin(ip, null);
        Optional<Usuario> usuario = buscarAtivo(cpf);
        if (usuario.isEmpty()) {
            return;
        }
        try {
            envioCodigoService.enviar(usuario.get(), FinalidadeEnvio.RECUPERACAO_SENHA);
            auditoriaService.registrar(
                    RegistroAuditoria.de(usuario.get().getId(), AcaoAuditoria.SENHA_RECUPERACAO_SOLICITADA));
        } catch (MuitasTentativasException e) {
            // Em espera de reenvio: não envia, mas responde igual (não revela que o CPF existe).
        }
    }

    @Transactional(noRollbackFor = CodigoInvalidoException.class)
    public void redefinir(String cpf, String codigo, String novaSenha, String ip) {
        String cpfHash = CpfUtils.valido(cpf) ? cpfHasher.calcular(cpf) : null;
        limiteTentativasService.verificarLogin(ip, cpfHash);
        // Valida a senha antes de consumir o código: senha fraca não gasta o código.
        PoliticaSenhaValidator.exigirValida(novaSenha);
        Optional<Usuario> encontrado = buscarAtivo(cpf);
        if (encontrado.isEmpty()
                || !envioCodigoService.verificar(encontrado.get(), FinalidadeEnvio.RECUPERACAO_SENHA, codigo)) {
            limiteTentativasService.registrarFalha(cpfHash);
            throw new CodigoInvalidoException();
        }
        Usuario usuario = encontrado.get();
        usuario.definirSenha(passwordEncoder.encode(novaSenha), false, Instant.now(clock));
        sessaoService.revogarTodas(usuario.getId());
        auditoriaService.registrar(RegistroAuditoria.sobre(
                usuario.getId(), AcaoAuditoria.SENHA_REDEFINIDA, "Usuario", usuario.getId()));
    }

    private Optional<Usuario> buscarAtivo(String cpf) {
        if (!CpfUtils.valido(cpf)) {
            return Optional.empty();
        }
        return usuarioRepository.findByCpfHash(cpfHasher.calcular(cpf)).filter(Usuario::ativo);
    }
}
