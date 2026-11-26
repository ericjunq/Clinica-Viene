package br.com.clinicaviene.identidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.compartilhado.cripto.HashCpf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Primeira subida com banco vazio e DEV_INICIAL_* definidos (D-048): cria o DEV uma única vez. Propriedades
 * próprias = contexto e banco (container) próprios, vazios como numa instalação nova.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "clinicaviene.dev-inicial.cpf=526.018.159-06",
            "clinicaviene.dev-inicial.nome=Dev Ficticio",
            "clinicaviene.dev-inicial.email=dev.ficticio@exemplo.com",
            "clinicaviene.dev-inicial.senha=Senha#Inicial1"
        })
@ActiveProfiles("test")
class InicializadorDevIT {

    @Autowired
    private RepositorioUsuarios repositorio;

    @Autowired
    private InicializadorDev inicializador;

    @Autowired
    private HashCpf hashCpf;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void criaODevUmaUnicaVezComTrocaDeSenhaObrigatoria() {
        Usuario dev = repositorio.findByCpfHash(hashCpf.calcular("52601815906")).orElseThrow();

        assertThat(dev.getPerfil()).isEqualTo(Perfil.DEV);
        assertThat(dev.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(dev.isTrocarSenha()).isTrue();
        assertThat(dev.isTotpAtivo()).isFalse();
        assertThat(dev.getCriadoPor()).isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit_log WHERE acao = 'USUARIO_INICIAL_CRIADO' AND entidade_id = ?",
                        Integer.class,
                        dev.getId().toString()))
                .isEqualTo(1);

        // Rodar de novo (ex.: reinício da aplicação) não cria nada.
        inicializador.run(new DefaultApplicationArguments());

        assertThat(repositorio.count()).isEqualTo(1);
    }
}
