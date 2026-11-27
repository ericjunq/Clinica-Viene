package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.FichasDeTeste;
import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.AssinarFichaRequest;
import br.com.clinicaviene.dto.AssinaturaRequest;
import br.com.clinicaviene.dto.DocumentoBaixadoDto;
import br.com.clinicaviene.dto.FichaAssinadaResponse;
import br.com.clinicaviene.dto.FichaCadastroRequest;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.ResponsavelLegalDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.Parentesco;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.exceptions.CpfNaoConfereException;
import br.com.clinicaviene.exceptions.LinkDocumentoInvalidoException;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.exceptions.SessaoCadastroInvalidaException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.service.ProvedorAssinaturaInternoTest.AssinaturasDeTeste;
import br.com.clinicaviene.util.CpfHasher;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Assinatura da ficha (2F — D-102) e entrega do PDF por link + CPF (D-094), com storage e e-mail reais. */
@Import({TestcontainersConfiguration.class, RelogioAjustavel.Configuracao.class})
@SpringBootTest
@ActiveProfiles("test")
class FichaAssinaturaServiceIT {

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final String IP = "203.0.113.20";
    private static final String APARELHO = "Navegador de teste";

    @Autowired
    private CadastroPublicoService cadastroPublicoService;

    @Autowired
    private EntregaDocumentoService entregaDocumentoService;

    @Autowired
    private ConviteService conviteService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private TermoService termoService;

    @Autowired
    private ModeloAnamneseService modeloAnamneseService;

    @Autowired
    private ArmazenamentoService armazenamentoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private JdbcTemplate jdbc;

    private UsuarioAutenticado recepcao;

    @BeforeEach
    void publicarTermosEModeloSeFaltarem() {
        recepcao = usuario(Perfil.ADMIN);
        UsuarioAutenticado master = usuario(Perfil.MASTER);
        if (termoService.vigente(TipoTermo.POLITICA_PRIVACIDADE).isEmpty()) {
            TermoVersao t = termoService.criarRascunho(TipoTermo.POLITICA_PRIVACIDADE, "Política", "Texto da política", master);
            termoService.publicar(t.getId(), master);
        }
        if (modeloAnamneseService.vigente(AreaAtuacao.ODONTOLOGIA).isEmpty()) {
            ModeloAnamnese m = modeloAnamneseService.criarRascunho(
                    AreaAtuacao.ODONTOLOGIA,
                    "Ficha de teste",
                    List.of(new PerguntaAnamneseDto(
                            "alergia", "Saúde", "Tem alergia?", TipoPergunta.SIM_NAO_DETALHE, true, null, true, true, null)),
                    master);
            modeloAnamneseService.publicar(m.getId(), master);
        }
    }

    @Test
    void pacienteAssinaPeloLinkEBaixaOPdfComOCpf() {
        String email = "julia." + UUID.randomUUID() + "@exemplo.com";
        String cpf = CpfsFicticios.gerar();
        UUID paciente = adulto(email, cpf);
        String convite = convite(paciente, ModoConvite.LINK);
        cadastroPublicoService.enviarCodigo(convite, IP, APARELHO);
        String sessao = cadastroPublicoService.confirmarCodigo(convite, codigo(email), IP, APARELHO).token();
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        FichaCadastroRequest ficha = FichasDeTeste.completa(formulario, "77988887777");

        FichaAssinadaResponse assinada = cadastroPublicoService.assinar(
                convite, sessao, new AssinarFichaRequest(ficha, digitado(formulario.dados().nomeCompleto()), true), IP,
                APARELHO);

        assertThat(assinada.sha256()).hasSize(64);
        assertThat(assinada.entregaEnviadaPara()).isEqualTo("j***@exemplo.com");

        // ficha imutável, respostas cifradas, documento no storage cifrado e conferível pelo hash
        Map<String, Object> linha = jdbc.queryForMap(
                "SELECT respostas, alertas, origem, pelo_responsavel FROM ficha_anamnese WHERE id = ?", assinada.fichaId());
        assertThat((String) linha.get("respostas")).doesNotContain("alergia").doesNotContain("Dipirona");
        assertThat(linha.get("origem")).isEqualTo("PACIENTE_LINK");
        assertThat(linha.get("pelo_responsavel")).isEqualTo(false);
        String chave = jdbc.queryForObject(
                "SELECT chave_armazenamento FROM documento WHERE id = ?", String.class, assinada.documentoId());
        byte[] pdf = armazenamentoService.ler(chave);
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(FichaAnamneseService.sha256(pdf)).isEqualTo(assinada.sha256());
        assertThatThrownBy(() -> jdbc.update("UPDATE ficha_anamnese SET origem = 'X' WHERE id = ?", assinada.fichaId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM documento WHERE id = ?", assinada.documentoId()))
                .isInstanceOf(DataAccessException.class);
        String evidencias = jdbc.queryForObject(
                "SELECT evidencias FROM assinatura WHERE documento_id = ?", String.class, assinada.documentoId());
        assertThat(evidencias).doesNotContain(IP);

        // dados conferidos gravados com versão; decisões sobre os termos registradas; convite encerrado
        assertThat(jdbc.queryForObject("SELECT telefone FROM paciente WHERE id = ?", String.class, paciente))
                .isEqualTo("77988887777");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM paciente_versao WHERE paciente_id = ? AND origem = 'PACIENTE_LINK'",
                        Integer.class, paciente))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM aceite_termo WHERE paciente_id = ?", Integer.class, paciente))
                .isEqualTo(formulario.termos().size());
        assertThat(contarAuditoria("FICHA_ASSINADA", paciente)).isEqualTo(1);
        assertThatThrownBy(() -> cadastroPublicoService.formulario(convite, sessao))
                .isInstanceOf(SessaoCadastroInvalidaException.class);

        // download: CPF errado conta tentativa; certo libera o mesmo PDF
        String link = linkDeDownload(email);
        assertThatThrownBy(() -> entregaDocumentoService.baixar(link, CpfsFicticios.gerar(), IP))
                .isInstanceOf(CpfNaoConfereException.class);
        DocumentoBaixadoDto baixado = entregaDocumentoService.baixar(link, cpf, IP);
        assertThat(baixado.conteudo()).isEqualTo(pdf);
        assertThat(contarAuditoria("DOCUMENTO_CPF_INCORRETO", paciente)).isEqualTo(1);
        assertThat(contarAuditoria("DOCUMENTO_BAIXADO", paciente)).isEqualTo(1);
    }

    @Test
    void menorAssinadoPeloResponsavelNoQuiosqueComDesenho() {
        String emailMae = "mae." + UUID.randomUUID() + "@exemplo.com";
        String cpfMae = CpfsFicticios.gerar();
        UUID crianca = pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(
                                "Leo Assinatura " + UUID.randomUUID(), null, pacienteService.hoje().minusYears(7), null,
                                "77999990000", null,
                                new ResponsavelLegalDto("Mãe do Leo", cpfMae, Parentesco.MAE, "77999991111", emailMae),
                                null, true),
                        recepcao)
                .getId();
        String convite = convite(crianca, ModoConvite.QUIOSQUE);
        String sessao = cadastroPublicoService.iniciarQuiosque(convite, IP, APARELHO).token();
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);

        FichaAssinadaResponse assinada = cadastroPublicoService.assinar(
                convite, sessao,
                new AssinarFichaRequest(
                        FichasDeTeste.completa(formulario, "77999990000"),
                        new AssinaturaRequest(MetodoAssinatura.DESENHO, AssinaturasDeTeste.desenhada(), null),
                        true),
                IP, APARELHO);

        assertThat(assinada.entregaEnviadaPara()).isEqualTo("m***@exemplo.com");
        assertThat(jdbc.queryForObject(
                        "SELECT tipo_signatario FROM assinatura WHERE documento_id = ?", String.class, assinada.documentoId()))
                .isEqualTo("RESPONSAVEL_LEGAL");
        assertThat(jdbc.queryForObject(
                        "SELECT nome_signatario FROM assinatura WHERE documento_id = ?", String.class, assinada.documentoId()))
                .isEqualTo("Mãe do Leo");
        assertThat(jdbc.queryForObject(
                        "SELECT origem FROM ficha_anamnese WHERE id = ?", String.class, assinada.fichaId()))
                .isEqualTo("PACIENTE_QUIOSQUE");
        // menor sem CPF: o download pede o CPF do responsável
        assertThatCode(() -> entregaDocumentoService.baixar(linkDeDownload(emailMae), cpfMae, IP))
                .doesNotThrowAnyException();
    }

    @Test
    void semDeclaracaoOuComNomeErradoNadaEGravado() {
        UUID paciente = adulto("caio." + UUID.randomUUID() + "@exemplo.com", CpfsFicticios.gerar());
        String convite = convite(paciente, ModoConvite.QUIOSQUE);
        String sessao = cadastroPublicoService.iniciarQuiosque(convite, IP, APARELHO).token();
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        FichaCadastroRequest ficha = FichasDeTeste.completa(formulario, null);

        assertThatThrownBy(() -> cadastroPublicoService.assinar(
                        convite, sessao, new AssinarFichaRequest(ficha, digitado(formulario.dados().nomeCompleto()), false),
                        IP, APARELHO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("declaração");
        assertThatThrownBy(() -> cadastroPublicoService.assinar(
                        convite, sessao, new AssinarFichaRequest(ficha, digitado("Outra Pessoa"), true), IP, APARELHO))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM ficha_anamnese WHERE paciente_id = ?", Integer.class, paciente))
                .isZero();

        // a sessão continua valendo: corrigido, assina; assinar de novo não dá (convite de uso único)
        AssinarFichaRequest certo = new AssinarFichaRequest(ficha, digitado(formulario.dados().nomeCompleto()), true);
        cadastroPublicoService.assinar(convite, sessao, certo, IP, APARELHO);
        assertThatThrownBy(() -> cadastroPublicoService.assinar(convite, sessao, certo, IP, APARELHO))
                .isInstanceOf(SessaoCadastroInvalidaException.class);
    }

    @Test
    void linkDeDownloadExpiraEBloqueiaDepoisDeCincoCpfsErrados() {
        String email = "rui." + UUID.randomUUID() + "@exemplo.com";
        String cpf = CpfsFicticios.gerar();
        UUID paciente = adulto(email, cpf);
        String convite = convite(paciente, ModoConvite.QUIOSQUE);
        String sessao = cadastroPublicoService.iniciarQuiosque(convite, IP, APARELHO).token();
        FormularioCadastroResponse formulario = cadastroPublicoService.formulario(convite, sessao);
        cadastroPublicoService.assinar(
                convite, sessao,
                new AssinarFichaRequest(FichasDeTeste.completa(formulario, null), digitado(formulario.dados().nomeCompleto()), true),
                IP, APARELHO);
        String link = linkDeDownload(email);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> entregaDocumentoService.baixar(link, "000.000.000-00", IP))
                    .isInstanceOf(CpfNaoConfereException.class);
        }
        assertThatThrownBy(() -> entregaDocumentoService.baixar(link, cpf, IP))
                .isInstanceOf(LinkDocumentoInvalidoException.class);
        assertThatThrownBy(() -> entregaDocumentoService.baixar("inexistente", cpf, IP))
                .isInstanceOf(LinkDocumentoInvalidoException.class);
        assertThatThrownBy(() -> entregaDocumentoService.baixar(null, cpf, IP))
                .isInstanceOf(LinkDocumentoInvalidoException.class);

        // outro link (nova entrega) também expira em 7 dias
        String outroEmail = "lia." + UUID.randomUUID() + "@exemplo.com";
        String outroCpf = CpfsFicticios.gerar();
        UUID outro = adulto(outroEmail, outroCpf);
        String outroConvite = convite(outro, ModoConvite.QUIOSQUE);
        String outraSessao = cadastroPublicoService.iniciarQuiosque(outroConvite, IP, APARELHO).token();
        FormularioCadastroResponse outroFormulario = cadastroPublicoService.formulario(outroConvite, outraSessao);
        cadastroPublicoService.assinar(
                outroConvite, outraSessao,
                new AssinarFichaRequest(
                        FichasDeTeste.completa(outroFormulario, null), digitado(outroFormulario.dados().nomeCompleto()), true),
                IP, APARELHO);
        String outroLink = linkDeDownload(outroEmail);
        relogio.avancar(Duration.ofDays(7));
        assertThatThrownBy(() -> entregaDocumentoService.baixar(outroLink, outroCpf, IP))
                .isInstanceOf(LinkDocumentoInvalidoException.class);
    }

    // --- apoio ---

    private static AssinaturaRequest digitado(String nome) {
        return new AssinaturaRequest(MetodoAssinatura.NOME_DIGITADO, null, nome);
    }

    private String convite(UUID paciente, ModoConvite modo) {
        String link = conviteService
                .criar(paciente, modo, AreaAtuacao.ODONTOLOGIA, false, modo == ModoConvite.QUIOSQUE, recepcao)
                .link();
        return link.substring(link.lastIndexOf('#') + 1);
    }

    private UUID adulto(String email, String cpf) {
        String nome = Character.toUpperCase(email.charAt(0)) + email.substring(1, email.indexOf('.')) + " Assinatura "
                + UUID.randomUUID();
        return pacienteService
                .preCadastrar(
                        new NovoPacienteRequest(nome, cpf, LocalDate.of(1988, 4, 2), email, null, null, null, null, false),
                        recepcao)
                .getId();
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil, null,
                null, null, null, "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(
                new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, relogio.instant()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }

    private int contarAuditoria(String acao, UUID entidadeId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE acao = ? AND entidade_id = ?", Integer.class, acao, entidadeId.toString());
        return total == null ? 0 : total;
    }

    private static String codigo(String email) {
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(texto(ultimaMensagem(email)));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static String linkDeDownload(String email) {
        Matcher m = Pattern.compile("/documento#([A-Za-z0-9_-]+)").matcher(texto(ultimaMensagem(email)));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static String texto(MimeMessage mensagem) {
        try {
            return mensagem.getContent().toString();
        } catch (IOException | MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MimeMessage ultimaMensagem(String email) {
        return Arrays.stream(GREENMAIL.getReceivedMessages())
                .filter(m -> {
                    try {
                        return Arrays.stream(m.getAllRecipients()).anyMatch(a -> a.toString().contains(email));
                    } catch (MessagingException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .reduce((primeira, segunda) -> segunda)
                .orElseThrow(() -> new AssertionError("Nenhum e-mail para " + email));
    }
}
