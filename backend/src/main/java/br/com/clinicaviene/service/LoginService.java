package br.com.clinicaviene.service;

import br.com.clinicaviene.auth.AuthProperties;
import br.com.clinicaviene.dto.LoginResponse;
import br.com.clinicaviene.dto.RegistroAuditoria;
import br.com.clinicaviene.entity.DesafioLogin;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AcaoAuditoria;
import br.com.clinicaviene.enums.EtapaLogin;
import br.com.clinicaviene.exceptions.CredenciaisInvalidasException;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.repository.DesafioLoginRepository;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import br.com.clinicaviene.util.CpfUtils;
import br.com.clinicaviene.util.TokenUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primeiro passo do login (D-071): CPF + senha. Se a senha estiver certa, emite um desafio de uso único que
 * precisa ser concluído com o 2FA ({@link DoisFatoresService}). Nenhuma sessão é aberta aqui.
 *
 * <p>Falhas são sempre a mesma resposta genérica e levam o mesmo tempo (o hash é verificado mesmo quando o
 * CPF não existe), para não revelar quais CPFs têm cadastro.
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final DesafioLoginRepository desafioLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final CpfHasher cpfHasher;
    private final AuditoriaService auditoriaService;
    private final LimiteTentativasService limiteTentativasService;
    private final AuthProperties authProperties;
    private final Clock clock;

    /** Hash de uma senha qualquer, para gastar o mesmo tempo quando o CPF não existe. */
    private volatile String hashFicticio;

    @Transactional
    public LoginResponse login(String cpf, String senha, String ip) {
        String cpfHash = CpfUtils.valido(cpf) ? cpfHasher.calcular(cpf) : null;
        try {
            limiteTentativasService.verificarLogin(ip, cpfHash);
        } catch (MuitasTentativasException e) {
            auditoriaService.registrarIndependente(
                    RegistroAuditoria.de(null, AcaoAuditoria.LOGIN_BLOQUEADO_TENTATIVAS));
            throw e;
        }

        Optional<Usuario> encontrado = cpfHash == null ? Optional.empty() : usuarioRepository.findByCpfHash(cpfHash);
        boolean senhaCorreta =
                passwordEncoder.matches(senha, encontrado.map(Usuario::senhaHash).orElseGet(this::hashFicticio));
        if (encontrado.isEmpty() || !senhaCorreta || !encontrado.get().ativo()) {
            limiteTentativasService.registrarFalha(cpfHash);
            auditoriaService.registrarIndependente(RegistroAuditoria.de(
                    encontrado.map(Usuario::getId).orElse(null), AcaoAuditoria.LOGIN_FALHA));
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = encontrado.get();
        Instant agora = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Instant expiraEm = agora.plus(authProperties.duracaoDesafio());
        String desafio = TokenUtils.gerar();
        desafioLoginRepository.save(new DesafioLogin(usuario.getId(), TokenUtils.sha256(desafio), agora, expiraEm));
        auditoriaService.registrar(RegistroAuditoria.de(usuario.getId(), AcaoAuditoria.LOGIN_SENHA_OK));
        EtapaLogin etapa = usuario.isTotpAtivo() ? EtapaLogin.VERIFICAR_2FA : EtapaLogin.CADASTRAR_2FA;
        return new LoginResponse(desafio, etapa, expiraEm);
    }

    private String hashFicticio() {
        if (hashFicticio == null) {
            hashFicticio = passwordEncoder.encode(TokenUtils.gerar());
        }
        return hashFicticio;
    }
}
