package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.CpfsFicticios;
import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.dto.NovoUsuarioDto;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.UsuarioAutenticado;
import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.Usuario;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.Perfil;
import br.com.clinicaviene.enums.StatusVersao;
import br.com.clinicaviene.enums.TipoPergunta;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.repository.UsuarioRepository;
import br.com.clinicaviene.util.CpfHasher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

/** Modelos de anamnese versionados (D-089). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ModeloAnamneseServiceIT {

    @Autowired
    private ModeloAnamneseService servico;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CpfHasher cpfHasher;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void exemploOdontologicoViraRascunhoCompletoEDepoisSoDuplicando() {
        UsuarioAutenticado master = usuario(Perfil.MASTER);

        ModeloAnamnese exemplo = servico.carregarExemploOdontologia(master);

        assertThat(exemplo.getStatus()).isEqualTo(StatusVersao.RASCUNHO);
        assertThat(exemplo.getAreaAtuacao()).isEqualTo(AreaAtuacao.ODONTOLOGIA);
        assertThat(servico.perguntas(exemplo)).hasSizeGreaterThanOrEqualTo(40);
        assertThatThrownBy(() -> servico.carregarExemploOdontologia(master)).hasMessageContaining("duplicar");
    }

    @Test
    void versionaPublicaImutavelEDuplicaParaNovaVersao() {
        UsuarioAutenticado master = usuario(Perfil.MASTER);
        ModeloAnamnese v1 = servico.criarRascunho(AreaAtuacao.PSICOLOGIA, "Anamnese psicológica", perguntas("queixa"), master);
        servico.editar(v1.getId(), "Anamnese psicológica", perguntas("queixa", "historico_familiar"), master);
        servico.publicar(v1.getId(), master);

        assertThat(servico.vigente(AreaAtuacao.PSICOLOGIA).orElseThrow().getId()).isEqualTo(v1.getId());
        assertThatThrownBy(() -> servico.editar(v1.getId(), "x", perguntas("a1"), master)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE modelo_anamnese SET titulo = 'x' WHERE id = ?", v1.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("publicado não pode ser alterado");

        ModeloAnamnese v2 = servico.duplicar(v1.getId(), master);
        assertThat(v2.getVersao()).isEqualTo(v1.getVersao() + 1);
        assertThat(v2.getStatus()).isEqualTo(StatusVersao.RASCUNHO);
        assertThat(servico.perguntas(v2)).extracting(PerguntaAnamneseDto::codigo).containsExactly("queixa", "historico_familiar");
        // a vigente continua a v1 até a v2 ser publicada
        assertThat(servico.vigente(AreaAtuacao.PSICOLOGIA).orElseThrow().getId()).isEqualTo(v1.getId());
    }

    @Test
    void soOMasterConfiguraERecusaModeloInvalido() {
        assertThatThrownBy(() -> servico.criarRascunho(AreaAtuacao.MEDICINA, "x", perguntas("a1"), usuario(Perfil.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servico.listar(usuario(Perfil.PROFISSIONAL))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servico.criarRascunho(AreaAtuacao.MEDICINA, "x", List.of(), usuario(Perfil.MASTER)))
                .isInstanceOf(RegraNegocioException.class);
    }

    private static List<PerguntaAnamneseDto> perguntas(String... codigos) {
        return java.util.Arrays.stream(codigos)
                .map(c -> new PerguntaAnamneseDto(c, "Geral", "Pergunta " + c + "?", TipoPergunta.TEXTO_LONGO, false, null, false, false, null))
                .toList();
    }

    private UsuarioAutenticado usuario(Perfil perfil) {
        boolean profissional = perfil == Perfil.PROFISSIONAL;
        NovoUsuarioDto dados = new NovoUsuarioDto(
                "Usuário " + perfil, CpfsFicticios.gerar(), "u." + UUID.randomUUID() + "@exemplo.com", null, perfil,
                profissional ? AreaAtuacao.ODONTOLOGIA : null,
                profissional ? br.com.clinicaviene.enums.ConselhoProfissional.CRO : null,
                profissional ? String.valueOf(System.nanoTime() % 1_000_000_000L) : null,
                profissional ? "BA" : null,
                "Senha#Provisoria1");
        Usuario u = usuarioRepository.saveAndFlush(new Usuario(dados, cpfHasher.calcular(dados.cpf()), "hash", null, Instant.now()));
        return new UsuarioAutenticado(u.getId(), u.getPerfil());
    }
}
