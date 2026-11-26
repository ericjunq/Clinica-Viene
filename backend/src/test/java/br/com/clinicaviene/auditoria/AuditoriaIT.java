package br.com.clinicaviene.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Auditoria somente-inclusão (D-076). Como o banco proíbe DELETE no audit_log, cada teste usa um
 * {@code entidade_id} aleatório para isolar as próprias linhas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class AuditoriaIT {

    @Autowired
    private ServicoAuditoria auditoria;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transacao;

    @Test
    void gravaORegistroComDataEAcaoComoTexto() {
        UUID usuario = UUID.randomUUID();
        String alvo = UUID.randomUUID().toString();

        auditoria.registrar(RegistroAuditoria.sobre(usuario, AcaoAuditoria.USUARIO_CRIADO, "Usuario", alvo));

        var linha = jdbc.queryForMap("SELECT * FROM audit_log WHERE entidade_id = ?", alvo);
        assertThat(linha.get("acao")).isEqualTo("USUARIO_CRIADO");
        assertThat(linha.get("usuario_id")).isEqualTo(usuario);
        assertThat(linha.get("entidade")).isEqualTo("Usuario");
        assertThat(linha.get("ocorrido_em")).isNotNull();
    }

    @Test
    void bancoRejeitaUpdateDeleteETruncate() {
        String alvo = UUID.randomUUID().toString();
        auditoria.registrar(RegistroAuditoria.sobre(null, AcaoAuditoria.LOGIN_FALHA, "Usuario", alvo));

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_log SET acao = 'LOGOUT' WHERE entidade_id = ?", alvo))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inclusão");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE entidade_id = ?", alvo))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inclusão");
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE audit_log"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("somente inclusão");

        assertThat(contar(alvo)).isEqualTo(1);
    }

    @Test
    void registroNaTransacaoDaOperacaoSomeNoRollback() {
        String alvo = UUID.randomUUID().toString();

        transacao.executeWithoutResult(status -> {
            auditoria.registrar(RegistroAuditoria.sobre(null, AcaoAuditoria.USUARIO_CRIADO, "Usuario", alvo));
            status.setRollbackOnly();
        });

        assertThat(contar(alvo)).isZero();
    }

    @Test
    void registroIndependenteFicaMesmoComRollbackDaOperacao() {
        String alvo = UUID.randomUUID().toString();

        transacao.executeWithoutResult(status -> {
            auditoria.registrarIndependente(
                    RegistroAuditoria.sobre(null, AcaoAuditoria.LOGIN_FALHA, "Usuario", alvo));
            status.setRollbackOnly();
        });

        assertThat(contar(alvo)).isEqualTo(1);
    }

    private int contar(String alvo) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entidade_id = ?", Integer.class, alvo);
        return total == null ? 0 : total;
    }
}
