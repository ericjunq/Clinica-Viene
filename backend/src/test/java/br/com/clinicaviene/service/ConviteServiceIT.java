package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.ConviteCriadoResponse;
import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.ResponsavelLegalDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ConviteCadastro;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ConselhoProfissional;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.Parentesco;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.SituacaoConvite;
import br.com.clinicaviene.exceptions.ConviteInvalidoException;
import br.com.clinicaviene.exceptions.MuitasTentativasException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

/** Convites de cadastro (D-091): link/QR, quiosque, um ativo por paciente, e-mail com regras da D-073. */
@Import({TestcontainersConfiguration.class, RelogioAjustavel.Configuracao.class})
@SpringBootTest
@ActiveProfiles("test")
class ConviteServiceIT {

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private ConviteService conviteService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void linkValeSetentaEDuasHorasEOTokenSoFicaComoHash() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        UUID paciente = adulto(recepcao, "link." + UUID.randomUUID() + "@exemplo.com");

        ConviteCriadoResponse criado = conviteService.criar(paciente, ModoConvite.LINK, false, false, recepcao);

        assertThat(criado.link()).startsWith("http://localhost:4200/cadastro/");
        assertThat(criado.qrCode()).startsWith("data:image/png;base64,");
        assertThat(Duration.between(relogio.instant(), criado.expiraEm())).isEqualTo(Duration.ofHours(72));
        assertThat(criado.emailEnviadoPara()).isNull();
        String token = token(criado);
        assertThat(conviteService.ativoPorToken(token).getPacienteId()).isEqualTo(paciente);
        Integer comTokenPuro = jdbc.queryForObject(
                "SELECT count(*) FROM convite_cadastro WHERE token_hash = ?", Integer.class, token);
        assertThat(comTokenPuro).isZero();
        assertThat(contarAuditoria("CONVITE_CRIADO", paciente)).isEqualTo(1);

        relogio.avancar(Duration.ofHours(72));
        assertThatThrownBy(() -> conviteService.ativoPorToken(token)).isInstanceOf(ConviteInvalidoException.class);
        assertThat(conviteService.listar(paciente, recepcao).getFirst().situacao(relogio.instant()))
                .isEqualTo(SituacaoConvite.EXPIRADO);
    }

    @Test
    void quiosqueExigeDocumentoConferidoValeDuasHorasENaoVaiPorEmail() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        UUID paciente = adulto(recepcao, "q." + UUID.randomUUID() + "@exemplo.com");

        assertThatThrownBy(() -> conviteService.criar(paciente, ModoConvite.QUIOSQUE, false, false, recepcao))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> conviteService.criar(paciente, ModoConvite.QUIOSQUE, true, true, recepcao))
                .isInstanceOf(RegraNegocioException.class);

        ConviteCriadoResponse criado = conviteService.criar(paciente, ModoConvite.QUIOSQUE, false, true, recepcao);
        assertThat(Duration.between(relogio.instant(), criado.expiraEm())).isEqualTo(Duration.ofHours(2));
        UUID conferidoPor = jdbc.queryForObject(
                "SELECT documento_conferido_por FROM convite_cadastro WHERE id = ?", UUID.class, criado.id());
        assertThat(conferidoPor).isEqualTo(recepcao.id());
    }

    @Test
    void novoConviteCancelaOAnteriorETokenTambemPodeSerCancelado() {
        UsuarioAutenticado recepcao = usuario(Perfil.MASTER);
        UUID paciente = adulto(recepcao, "n." + UUID.randomUUID() + "@exemplo.com");
        String primeiro = token(conviteService.criar(paciente, ModoConvite.LINK, false, false, recepcao));
        ConviteCriadoResponse segundo = conviteService.criar(paciente, ModoConvite.LINK, false, false, recepcao);

        assertThatThrownBy(() -> conviteService.ativoPorToken(primeiro)).isInstanceOf(ConviteInvalidoException.class);
        assertThatCode(() -> conviteService.ativoPorToken(token(segundo))).doesNotThrowAnyException();

        conviteService.cancelar(paciente, segundo.id(), recepcao);
        assertThatThrownBy(() -> conviteService.ativoPorToken(token(segundo))).isInstanceOf(ConviteInvalidoException.class);
        List<ConviteCadastro> convites = conviteService.listar(paciente, recepcao);
        assertThat(convites).extracting(c -> c.situacao(relogio.instant()))
                .containsOnly(SituacaoConvite.CANCELADO);
        assertThat(contarAuditoria("CONVITE_CANCELADO", paciente)).isEqualTo(1);

        assertThatThrownBy(() -> conviteService.cancelar(UUID.randomUUID(), segundo.id(), recepcao))
                .isInstanceOf(ConviteInvalidoException.class);
        assertThatThrownBy(() -> conviteService.ativoPorToken("")).isInstanceOf(ConviteInvalidoException.class);
        assertThatThrownBy(() -> conviteService.ativoPorToken("inexistente")).isInstanceOf(ConviteInvalidoException.class);
    }

    @Test
    void enviaEmailSemDadoDeSaudeComEsperaEntreReenvios() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        String email = "envio." + UUID.randomUUID() + "@exemplo.com";
        UUID paciente = adulto(recepcao, email);

        ConviteCriadoResponse criado = conviteService.criar(paciente, ModoConvite.LINK, true, false, recepcao);
        assertThat(criado.emailEnviadoPara()).isEqualTo(email.charAt(0) + "***@exemplo.com");
        assertThat(Duration.between(relogio.instant(), criado.podeReenviarEm())).isEqualTo(Duration.ofMinutes(1));
        String corpo = GreenMailUtil.getBody(ultimaMensagem(email));
        assertThat(corpo).contains(criado.link().substring(criado.link().lastIndexOf('/') + 1)).contains("72 horas");
        assertThat(contarAuditoria("CONVITE_ENVIADO_EMAIL", paciente)).isEqualTo(1);

        assertThatThrownBy(() -> conviteService.criar(paciente, ModoConvite.LINK, true, false, recepcao))
                .isInstanceOf(MuitasTentativasException.class);
        // a recusa não cancela o convite que já foi enviado
        assertThatCode(() -> conviteService.ativoPorToken(token(criado))).doesNotThrowAnyException();
        // sem e-mail (só QR) não há espera
        assertThatCode(() -> conviteService.criar(paciente, ModoConvite.LINK, false, false, recepcao))
                .doesNotThrowAnyException();

        relogio.avancar(Duration.ofMinutes(1));
        ConviteCriadoResponse reenviado = conviteService.criar(paciente, ModoConvite.LINK, true, false, recepcao);
        assertThat(Duration.between(relogio.instant(), reenviado.podeReenviarEm())).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void menorRecebeNoEmailDoResponsavelESemEmailNaoEnvia() {
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);
        String emailResponsavel = "mae." + UUID.randomUUID() + "@exemplo.com";
        UUID crianca = pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Criança Convite " + UUID.randomUUID(), CpfsFicticios.gerar(),
                                pacienteService.hoje().minusYears(8), null, "77999990000", null,
                                new ResponsavelLegalDto("Mãe Convite", CpfsFicticios.gerar(), Parentesco.MAE, "77999991111",
                                        emailResponsavel),
                                null, false),
                        recepcao)
                .getId();
        conviteService.criar(crianca, ModoConvite.LINK, true, false, recepcao);
        assertThat(ultimaMensagem(emailResponsavel)).isNotNull();

        UUID semEmail = pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Adulto Sem Email " + UUID.randomUUID(), CpfsFicticios.gerar(),
                                pacienteService.hoje().minusYears(40), null, "77999992222", null, null, null, false),
                        recepcao)
                .getId();
        assertThatThrownBy(() -> conviteService.criar(semEmail, ModoConvite.LINK, true, false, recepcao))
                .isInstanceOf(RegraNegocioException.class);
        assertThatCode(() -> conviteService.criar(semEmail, ModoConvite.LINK, false, false, recepcao))
                .doesNotThrowAnyException();
    }

    @Test
    void soRecepcaoEMasterGeramConvites() {
        UUID paciente = adulto(usuario(Perfil.ADMIN), "perm." + UUID.randomUUID() + "@exemplo.com");
        for (Perfil perfil : List.of(Perfil.PROFISSIONAL, Perfil.DEV)) {
            UsuarioAutenticado ator = usuario(perfil);
            assertThatThrownBy(() -> conviteService.criar(paciente, ModoConvite.LINK, false, false, ator))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> conviteService.listar(paciente, ator)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void mascaraEmail() {
        assertThat(ConviteService.mascararEmail("maria@exemplo.com")).isEqualTo("m***@exemplo.com");
        assertThat(ConviteService.mascararEmail("a@exemplo.com")).isEqualTo("***@exemplo.com");
    }

    // --- apoio ---

    private static String token(ConviteCriadoResponse criado) {
        return criado.link().substring(criado.link().lastIndexOf('/') + 1);
    }

    private UUID adulto(UsuarioAutenticado recepcao, String email) {
        return pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Paciente Convite " + UUID.randomUUID(), CpfsFicticios.gerar(),
                                pacienteService.hoje().minusYears(30), email, null,
                                new EnderecoDto("45000-000", "Rua A", "1", null, "Centro", "Vitória da Conquista", "BA"),
                                null, null, false),
                        recepcao)
                .getId();
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, relogio.instant()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }

    private int contarAuditoria(String acao, UUID entidadeId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE acao = ? AND entidade_id = ?", Integer.class, acao, entidadeId.toString());
        return total == null ? 0 : total;
    }

    private static MimeMessage ultimaMensagem(String email) {
        return Arrays.stream(GREENMAIL.getReceivedMessages())
                .filter(m -> Arrays.stream(destinatarios(m)).anyMatch(a -> a.toString().contains(email)))
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow(() -> new AssertionError("Nenhum e-mail para " + email));
    }

    private static jakarta.mail.Address[] destinatarios(MimeMessage mensagem) {
        try {
            return mensagem.getAllRecipients();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
