package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.FinalidadeEnvio;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Envio de códigos por e-mail com as regras de reenvio da D-073 (espera 1/2/5/10 min, 5 envios, 1 h). */
@Import({TestcontainersConfiguration.class, RelogioAjustavel.Configuracao.class})
@SpringBootTest
@ActiveProfiles("test")
class EnvioCodigoServiceIT {

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final FinalidadeEnvio FINALIDADE = FinalidadeEnvio.DOIS_FATORES_EMAIL;

    @Autowired
    private EnvioCodigoService envioCodigoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private RelogioAjustavel relogio;

    @Test
    void enviaCodigoPorEmailQueValeUmaVez() {
        Usuario usuario = criarUsuario();

        envioCodigoService.enviar(usuario, FINALIDADE);
        String codigo = ultimoCodigo(usuario);

        assertThat(envioCodigoService.verificar(usuario, FINALIDADE, codigo)).isTrue();
        assertThat(envioCodigoService.verificar(usuario, FINALIDADE, codigo)).isFalse();
    }

    @Test
    void esperaCrescenteEntreReenviosEBloqueioDeUmaHoraAposCincoEnvios() {
        Usuario usuario = criarUsuario();

        Instant liberado = envioCodigoService.enviar(usuario, FINALIDADE); // 1º envio
        assertThat(Duration.between(relogio.instant(), liberado)).isEqualTo(Duration.ofMinutes(1));
        assertBloqueado(usuario);

        relogio.avancar(Duration.ofMinutes(1));
        envioCodigoService.enviar(usuario, FINALIDADE); // 2º (após 1 min)
        relogio.avancar(Duration.ofMinutes(1));
        assertBloqueado(usuario); // precisa de 2 min
        relogio.avancar(Duration.ofMinutes(1));
        envioCodigoService.enviar(usuario, FINALIDADE); // 3º (após 2 min)
        relogio.avancar(Duration.ofMinutes(5));
        envioCodigoService.enviar(usuario, FINALIDADE); // 4º (após 5 min)
        relogio.avancar(Duration.ofMinutes(10));
        liberado = envioCodigoService.enviar(usuario, FINALIDADE); // 5º (após 10 min)
        assertThat(Duration.between(relogio.instant(), liberado)).isEqualTo(Duration.ofHours(1));

        relogio.avancar(Duration.ofMinutes(30));
        assertBloqueado(usuario); // 5 envios: só 1 h depois do último
        relogio.avancar(Duration.ofMinutes(30));
        assertThatCode(() -> envioCodigoService.enviar(usuario, FINALIDADE)).doesNotThrowAnyException();
    }

    @Test
    void envioNovoInvalidaOCodigoAnterior() {
        Usuario usuario = criarUsuario();
        envioCodigoService.enviar(usuario, FINALIDADE);
        String antigo = ultimoCodigo(usuario);
        relogio.avancar(Duration.ofMinutes(1));
        envioCodigoService.enviar(usuario, FINALIDADE);
        String novo = ultimoCodigo(usuario);

        if (!antigo.equals(novo)) {
            assertThat(envioCodigoService.verificar(usuario, FINALIDADE, antigo)).isFalse();
        }
        assertThat(envioCodigoService.verificar(usuario, FINALIDADE, novo)).isTrue();
    }

    @Test
    void cincoErrosInvalidamOCodigo() {
        Usuario usuario = criarUsuario();
        envioCodigoService.enviar(usuario, FINALIDADE);
        String codigo = ultimoCodigo(usuario);
        String errado = codigo.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThat(envioCodigoService.verificar(usuario, FINALIDADE, errado)).isFalse();
        }

        assertThat(envioCodigoService.verificar(usuario, FINALIDADE, codigo)).isFalse();
    }

    @Test
    void codigoExpiraEmDezMinutos() {
        Usuario usuario = criarUsuario();
        envioCodigoService.enviar(usuario, FINALIDADE);
        String codigo = ultimoCodigo(usuario);

        relogio.avancar(Duration.ofMinutes(10));

        assertThat(envioCodigoService.verificar(usuario, FINALIDADE, codigo)).isFalse();
    }

    @Test
    void finalidadesTemLimitesIndependentes() {
        Usuario usuario = criarUsuario();
        envioCodigoService.enviar(usuario, FINALIDADE);

        assertThatCode(() -> envioCodigoService.enviar(usuario, FinalidadeEnvio.RECUPERACAO_SENHA))
                .doesNotThrowAnyException();
    }

    @Test
    void emailNaoTemDadoAlemDoCodigo() throws Exception {
        Usuario usuario = criarUsuario();
        envioCodigoService.enviar(usuario, FINALIDADE);

        MimeMessage mensagem = ultimaMensagem(usuario);
        String corpo = GreenMailUtil.getBody(mensagem);
        assertThat(mensagem.getSubject()).contains("código de acesso");
        assertThat(corpo).contains("10 minutos").doesNotContain(usuario.getCpf()).doesNotContain(usuario.getNomeCompleto());
    }

    // --- apoio ---

    private void assertBloqueado(Usuario usuario) {
        assertThatThrownBy(() -> envioCodigoService.enviar(usuario, FINALIDADE))
                .isInstanceOf(MuitasTentativasException.class);
    }

    private Usuario criarUsuario() {
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa Envio",
                CpfsFicticios.gerar(),
                "envio." + UUID.randomUUID() + "@exemplo.com",
                null,
                Perfil.ADMIN,
                null,
                null,
                null,
                null,
                "Senha#Provisoria1");
        return usuarioRepository.saveAndFlush(
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash-de-teste", null, relogio.instant()));
    }

    private MimeMessage ultimaMensagem(Usuario usuario) {
        return Arrays.stream(GREENMAIL.getReceivedMessages())
                .filter(m -> Arrays.stream(recipients(m)).anyMatch(a -> a.toString().contains(usuario.getEmail())))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow(() -> new AssertionError("Nenhum e-mail para " + usuario.getEmail()));
    }

    private String ultimoCodigo(Usuario usuario) {
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(GreenMailUtil.getBody(ultimaMensagem(usuario)));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static jakarta.mail.Address[] recipients(MimeMessage mensagem) {
        try {
            return mensagem.getAllRecipients();
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
