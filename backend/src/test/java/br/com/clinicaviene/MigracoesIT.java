package br.com.clinicaviene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Confere que as migrations da Fase 0 foram aplicadas pelo Flyway (D-049, D-050, D-059). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class MigracoesIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void criaExtensaoBtreeGist() {
        Integer total =
                jdbc.queryForObject("SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class);
        assertThat(total).isEqualTo(1);
    }

    @Test
    void criaTabelaDeEventosDoModulith() {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'event_publication'",
                Integer.class);
        assertThat(total).isEqualTo(1);
    }

    @Test
    void criaTabelaDeArquivoDeEventosDoModulith() {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'event_publication_archive'",
                Integer.class);
        assertThat(total).isEqualTo(1);
    }
}
