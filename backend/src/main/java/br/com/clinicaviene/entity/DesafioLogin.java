package br.com.clinicaviene.entity;

import br.com.clinicaviene.util.IdUtils;
import br.com.clinicaviene.util.CampoCriptografadoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Desafio do login em 2 passos (D-071): prova que a senha foi aceita e é exigido no passo do 2FA. Uso
 * único, expira em minutos e morre após o número máximo de códigos errados.
 */
@Entity
@Table(name = "desafio_login")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DesafioLogin {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    /** Segredo TOTP gerado no cadastro do 2FA, ainda não confirmado. Cifrado (AES-GCM). */
    @Getter(AccessLevel.NONE)
    @Convert(converter = CampoCriptografadoConverter.class)
    @Column(name = "totp_segredo_pendente")
    private String totpSegredoPendente;

    public DesafioLogin(UUID usuarioId, String tokenHash, Instant agora, Instant expiraEm) {
        this.id = IdUtils.novoId();
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.tentativas = 0;
        this.criadoEm = agora;
        this.expiraEm = expiraEm;
    }

    /** Ainda pode ser usado: não concluído, não expirado e com tentativas sobrando. */
    public boolean valido(Instant agora, int maximoTentativas) {
        return concluidoEm == null && agora.isBefore(expiraEm) && tentativas < maximoTentativas;
    }

    public void registrarFalha() {
        this.tentativas++;
    }

    public void concluir(Instant agora) {
        this.concluidoEm = agora;
        this.totpSegredoPendente = null;
    }

    public void definirSegredoPendente(String segredoBase32) {
        this.totpSegredoPendente = segredoBase32;
    }

    public String segredoPendente() {
        return totpSegredoPendente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DesafioLogin outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return DesafioLogin.class.hashCode();
    }
}
