package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sessão em que o paciente preenche o cadastro (D-100). No modo LINK começa pendente, com um código enviado ao
 * e-mail; confirmado o código, recebe o token da sessão. No quiosque já nasce confirmada.
 */
@Entity
@Table(name = "sessao_cadastro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessaoCadastro {

    @Id
    private UUID id;

    @Column(name = "convite_id", nullable = false)
    private UUID conviteId;

    @Column(name = "codigo_hash")
    private String codigoHash;

    @Column(name = "codigo_expira_em")
    private Instant codigoExpiraEm;

    @Column(name = "tentativas_codigo", nullable = false)
    private int tentativasCodigo;

    @Column(name = "codigo_enviado_em")
    private Instant codigoEnviadoEm;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "confirmada_em")
    private Instant confirmadaEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "ip")
    private String ip;

    @Column(name = "dispositivo")
    private String dispositivo;

    private SessaoCadastro(UUID conviteId, String ip, String dispositivo, Instant agora) {
        this.id = IdUtils.novoId();
        this.conviteId = conviteId;
        this.ip = ip;
        this.dispositivo = dispositivo == null || dispositivo.length() <= 300 ? dispositivo : dispositivo.substring(0, 300);
        this.criadaEm = agora;
    }

    /** LINK: sessão pendente, aguardando o código enviado ao e-mail. */
    public static SessaoCadastro pendente(
            UUID conviteId, String codigoHash, Instant codigoExpiraEm, String ip, String dispositivo, Instant agora) {
        SessaoCadastro s = new SessaoCadastro(conviteId, ip, dispositivo, agora);
        s.codigoHash = codigoHash;
        s.codigoExpiraEm = codigoExpiraEm;
        s.codigoEnviadoEm = agora;
        return s;
    }

    /** QUIOSQUE: identidade já conferida pela recepção. */
    public static SessaoCadastro confirmadaNoQuiosque(
            UUID conviteId, String tokenHash, Instant expiraEm, String ip, String dispositivo, Instant agora) {
        SessaoCadastro s = new SessaoCadastro(conviteId, ip, dispositivo, agora);
        s.confirmar(tokenHash, expiraEm, agora);
        return s;
    }

    /** O código ainda pode ser conferido (não expirou, não esgotou tentativas, não foi usado). */
    public boolean codigoUtilizavel(Instant agora, int tentativasPermitidas) {
        return codigoHash != null
                && confirmadaEm == null
                && tentativasCodigo < tentativasPermitidas
                && agora.isBefore(codigoExpiraEm);
    }

    public void registrarFalhaCodigo() {
        tentativasCodigo++;
    }

    public void confirmar(String tokenHash, Instant expiraEm, Instant agora) {
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
        this.confirmadaEm = agora;
    }

    public boolean valida(Instant agora) {
        return confirmadaEm != null && agora.isBefore(expiraEm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof SessaoCadastro outra && id != null && Objects.equals(id, outra.id);
    }

    @Override
    public int hashCode() {
        return SessaoCadastro.class.hashCode();
    }
}
