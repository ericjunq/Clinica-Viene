package br.com.clinicaviene.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.RelogioAjustavel;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.compartilhado.cripto.CpfHasher;
import br.com.clinicaviene.identidade.dto.NovoUsuarioDto;
import br.com.clinicaviene.identidade.dto.SessaoDto;
import br.com.clinicaviene.identidade.entity.Usuario;
import br.com.clinicaviene.identidade.enums.AreaAtuacao;
import br.com.clinicaviene.identidade.enums.ConselhoProfissional;
import br.com.clinicaviene.identidade.enums.Perfil;
import br.com.clinicaviene.identidade.exceptions.RenovacaoConcorrenteException;
import br.com.clinicaviene.identidade.exceptions.SessaoInvalidaException;
import br.com.clinicaviene.identidade.repository.UsuarioRepository;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

/** Sessões: rotação, reuso, expiração e revogação do token de renovação (D-071, D-068). */
@Import({TestcontainersConfiguration.class, RelogioAjustavel.Configuracao.class})
@SpringBootTest
@ActiveProfiles("test")
public class SessaoServiceIT {

    @Autowired
    private SessaoService sessoes;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CpfHasher hashCpf;

    @Autowired
    private JwtDecoder decodificador;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void iniciaSessaoComJwtValidoETokenDeRenovacaoSoComoHash() {
        Usuario usuario = novoUsuario(Perfil.ADMIN);

        SessaoDto sessao = sessoes.iniciar(usuario);

        Jwt jwt = decodificador.decode(sessao.tokenAcesso());
        assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("clinica-viene");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM token_renovacao WHERE token_hash = ?", Integer.class, sessao.tokenRenovacao()))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM token_renovacao WHERE token_hash = ?",
                        Integer.class,
                        SessaoService.hash(sessao.tokenRenovacao())))
                .isEqualTo(1);
    }

    @Test
    void renovarTrocaOTokenMantendoOFimDaSessao() {
        SessaoDto primeira = sessoes.iniciar(novoUsuario(Perfil.ADMIN));
        relogio.avancar(Duration.ofMinutes(10));

        SessaoDto segunda = sessoes.renovar(primeira.tokenRenovacao());

        assertThat(segunda.tokenRenovacao()).isNotEqualTo(primeira.tokenRenovacao());
        assertThat(segunda.sessaoExpiraEm()).isEqualTo(primeira.sessaoExpiraEm());
        assertThat(sessoes.renovar(segunda.tokenRenovacao()).tokenRenovacao()).isNotBlank();
    }

    @Test
    void reusoLogoAposATrocaEhCorridaEntreAbasENaoDerrubaASessao() {
        SessaoDto primeira = sessoes.iniciar(novoUsuario(Perfil.ADMIN));
        SessaoDto segunda = sessoes.renovar(primeira.tokenRenovacao());

        relogio.avancar(Duration.ofSeconds(5));

        assertThatThrownBy(() -> sessoes.renovar(primeira.tokenRenovacao()))
                .isInstanceOf(RenovacaoConcorrenteException.class);
        assertThat(sessoes.renovar(segunda.tokenRenovacao())).isNotNull();
    }

    @Test
    void reusoForaDaToleranciaRevogaACadeiaInteiraEAudita() {
        Usuario usuario = novoUsuario(Perfil.ADMIN);
        SessaoDto primeira = sessoes.iniciar(usuario);
        SessaoDto segunda = sessoes.renovar(primeira.tokenRenovacao());

        relogio.avancar(Duration.ofSeconds(21));

        assertThatThrownBy(() -> sessoes.renovar(primeira.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);
        assertThatThrownBy(() -> sessoes.renovar(segunda.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit_log WHERE acao = 'SESSAO_REUSO_DETECTADO' AND usuario_id = ?",
                        Integer.class,
                        usuario.getId()))
                .isEqualTo(1);
    }

    @Test
    void sessaoExpiraDozeHorasAposOLoginMesmoRenovando() {
        SessaoDto sessao = sessoes.iniciar(novoUsuario(Perfil.ADMIN));
        relogio.avancar(Duration.ofHours(11));
        SessaoDto renovada = sessoes.renovar(sessao.tokenRenovacao());

        relogio.avancar(Duration.ofHours(1));

        assertThatThrownBy(() -> sessoes.renovar(renovada.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);
    }

    @Test
    void logoutRevogaEAudita() {
        Usuario usuario = novoUsuario(Perfil.ADMIN);
        SessaoDto sessao = sessoes.iniciar(usuario);

        sessoes.encerrar(sessao.tokenRenovacao());

        assertThatThrownBy(() -> sessoes.renovar(sessao.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit_log WHERE acao = 'LOGOUT' AND usuario_id = ?",
                        Integer.class,
                        usuario.getId()))
                .isEqualTo(1);
    }

    @Test
    void usuarioInativoNaoRenovaERevogarTodasDerrubaTodasAsSessoes() {
        Usuario usuario = novoUsuario(Perfil.PROFISSIONAL);
        SessaoDto aba1 = sessoes.iniciar(usuario);
        SessaoDto aba2 = sessoes.iniciar(usuario);

        sessoes.revogarTodas(usuario.getId());

        assertThatThrownBy(() -> sessoes.renovar(aba1.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);
        assertThatThrownBy(() -> sessoes.renovar(aba2.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);

        SessaoDto nova = sessoes.iniciar(usuario);
        jdbc.update("UPDATE usuario SET status = 'INATIVO' WHERE id = ?", usuario.getId());
        assertThatThrownBy(() -> sessoes.renovar(nova.tokenRenovacao()))
                .isInstanceOf(SessaoInvalidaException.class);
    }

    @Test
    void tokenDesconhecidoOuVazioEhInvalido() {
        assertThatThrownBy(() -> sessoes.renovar("nao-existe")).isInstanceOf(SessaoInvalidaException.class);
        assertThatThrownBy(() -> sessoes.renovar(null)).isInstanceOf(SessaoInvalidaException.class);
        assertThatThrownBy(() -> sessoes.renovar(" ")).isInstanceOf(SessaoInvalidaException.class);
    }

    private Usuario novoUsuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Pessoa Sessao",
                CpfsFicticios.gerar(),
                "sessao." + UUID.randomUUID() + "@exemplo.com",
                null,
                perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        return usuarios.saveAndFlush(
                new Usuario(dados, hashCpf.calcular(dados.cpf()), "hash-de-teste", null, relogio.instant()));
    }
}
