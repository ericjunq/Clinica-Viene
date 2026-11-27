package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.ConsentimentoResponse;
import br.com.clinicaviene.dto.DecisaoConsentimentoDto;
import br.com.clinicaviene.dto.EvidenciaAceiteDto;
import br.com.clinicaviene.dto.NovoPacienteRequest;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.Paciente;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.DecisaoTermo;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

/** Termos versionados e decisões do paciente (D-089, D-093). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class TermoEConsentimentoServiceIT {

    @Autowired
    private TermoService termoService;

    @Autowired
    private ConsentimentoService consentimentoService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private JdbcTemplate jdbc;

    private UsuarioAutenticado master;

    @BeforeEach
    void publicarTodosOsTermos() {
        master = usuario(Perfil.MASTER);
        for (TipoTermo tipo : TipoTermo.values()) {
            if (termoService.vigente(tipo).isEmpty()) {
                TermoVersao t = termoService.criarRascunho(tipo, "Termo " + tipo, "Texto do termo " + tipo, master);
                termoService.publicar(t.getId(), master);
            }
        }
    }

    @Test
    void rascunhoEditavelPublicadoImutavelInclusiveNoBanco() {
        TermoVersao rascunho = termoService.criarRascunho(TipoTermo.CONSENTIMENTO_PESQUISA, "Pesquisa", "v1", master);
        assertThat(rascunho.getStatus()).isEqualTo(StatusVersao.RASCUNHO);
        assertThat(termoService.editar(rascunho.getId(), "Pesquisa", "texto revisado", master).getTexto())
                .isEqualTo("texto revisado");

        TermoVersao publicado = termoService.publicar(rascunho.getId(), master);
        assertThat(termoService.vigente(TipoTermo.CONSENTIMENTO_PESQUISA).orElseThrow().getId()).isEqualTo(publicado.getId());

        assertThatThrownBy(() -> termoService.editar(publicado.getId(), "x", "y", master)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE termo_versao SET texto = 'alterado' WHERE id = ?", publicado.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("publicado não pode ser alterado");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM termo_versao WHERE id = ?", publicado.getId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void soOMasterConfiguraTermos() {
        assertThatThrownBy(() -> termoService.criarRascunho(TipoTermo.CONSENTIMENTO_PESQUISA, "x", "y", usuario(Perfil.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> termoService.listar(usuario(Perfil.DEV))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void carregarExemplosDeixaTodoTipoComPeloMenosUmaVersao() {
        termoService.carregarExemplos(master);

        assertThat(termoService.listar(master)).extracting(TermoVersao::getTipo).contains(TipoTermo.values());
    }

    @Test
    void registraAceitesERecusasComEvidenciasEExigeAPolitica() {
        Paciente paciente = paciente();
        EvidenciaAceiteDto evidencia = new EvidenciaAceiteDto(OrigemAlteracao.PACIENTE_LINK, false, "200.1.2.3", "Firefox/120", null);

        assertThatThrownBy(() -> consentimentoService.registrarDecisoes(
                        paciente.getId(), List.of(new DecisaoConsentimentoDto(TipoTermo.CONSENTIMENTO_LEMBRETES, true)), evidencia))
                .hasMessageContaining("política de privacidade");

        consentimentoService.registrarDecisoes(
                paciente.getId(),
                List.of(new DecisaoConsentimentoDto(TipoTermo.POLITICA_PRIVACIDADE, true),
                        new DecisaoConsentimentoDto(TipoTermo.CONSENTIMENTO_LEMBRETES, true)),
                evidencia);

        assertThat(consentimentoService.temConsentimentoAtivo(paciente.getId(), TipoTermo.CONSENTIMENTO_LEMBRETES)).isTrue();
        assertThat(consentimentoService.temConsentimentoAtivo(paciente.getId(), TipoTermo.CONSENTIMENTO_PROMOCOES)).isFalse();
        List<ConsentimentoResponse> situacao = consentimentoService.situacao(paciente.getId(), usuario(Perfil.ADMIN));
        assertThat(situacao).filteredOn(c -> c.tipo() == TipoTermo.CONSENTIMENTO_PROMOCOES)
                .singleElement()
                .extracting(ConsentimentoResponse::situacao)
                .isEqualTo(DecisaoTermo.RECUSADO); // oferecido desmarcado: fica a prova
        var linha = jdbc.queryForMap(
                "SELECT * FROM aceite_termo WHERE paciente_id = ? AND tipo = 'POLITICA_PRIVACIDADE'", paciente.getId());
        assertThat(linha.get("ip")).isEqualTo("200.1.2.3");
        assertThat(linha.get("dispositivo")).isEqualTo("Firefox/120");
        assertThat(linha.get("origem")).isEqualTo("PACIENTE_LINK");
    }

    @Test
    void revogaConsentimentoMasNaoAPoliticaEOHistoricoESomenteInclusao() {
        Paciente paciente = paciente();
        consentimentoService.registrarDecisoes(
                paciente.getId(),
                List.of(new DecisaoConsentimentoDto(TipoTermo.POLITICA_PRIVACIDADE, true),
                        new DecisaoConsentimentoDto(TipoTermo.CONSENTIMENTO_IMAGEM, true)),
                new EvidenciaAceiteDto(OrigemAlteracao.PACIENTE_QUIOSQUE, false, "10.0.0.1", "Tablet", null));
        UsuarioAutenticado recepcao = usuario(Perfil.ADMIN);

        assertThatThrownBy(() -> consentimentoService.revogar(paciente.getId(), TipoTermo.POLITICA_PRIVACIDADE, null, "1.1.1.1", recepcao))
                .hasMessageContaining("não se revoga");
        assertThatThrownBy(() -> consentimentoService.revogar(paciente.getId(), TipoTermo.CONSENTIMENTO_PROMOCOES, null, "1.1.1.1", recepcao))
                .hasMessageContaining("Não há consentimento ativo");

        consentimentoService.revogar(paciente.getId(), TipoTermo.CONSENTIMENTO_IMAGEM, "Pediu na recepção", "1.1.1.1", recepcao);

        assertThat(consentimentoService.temConsentimentoAtivo(paciente.getId(), TipoTermo.CONSENTIMENTO_IMAGEM)).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit_log WHERE acao = 'CONSENTIMENTO_REVOGADO' AND entidade_id = ?",
                        Integer.class,
                        paciente.getId().toString()))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM aceite_termo WHERE paciente_id = ?", paciente.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inclusão");
        assertThatThrownBy(() -> consentimentoService.situacao(paciente.getId(), usuario(Perfil.PROFISSIONAL)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- apoio ---

    private Paciente paciente() {
        return pacienteService.preCadastrar(
                new NovoPacienteRequest("Paciente Termo", CpfsFicticios.gerar(), pacienteService.hoje().minusYears(30),
                        "t." + UUID.randomUUID() + "@exemplo.com", null, null, null, null, false),
                usuario(Perfil.ADMIN));
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                profissional ? br.com.clinicaviene.enums.AreaAtuacao.ODONTOLOGIA : null,
                profissional ? br.com.clinicaviene.enums.ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, Instant.now()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }
}
