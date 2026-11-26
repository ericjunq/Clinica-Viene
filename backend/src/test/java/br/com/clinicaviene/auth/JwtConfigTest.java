package br.com.clinicaviene.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class JwtConfigTest {

    @Test
    void leChaveEmBase64DerOuPem() throws Exception {
        KeyPair par = gerar(2048);
        String base64 = Base64.getEncoder().encodeToString(par.getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(par.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        RSAKey deBase64 = JwtConfig.lerChave(base64);
        RSAKey dePem = JwtConfig.lerChave(pem);

        assertThat(deBase64.toRSAPublicKey()).isEqualTo(par.getPublic());
        assertThat(dePem.toRSAPublicKey()).isEqualTo(par.getPublic());
        assertThat(deBase64.getKeyID()).isEqualTo("v1");
        assertThat(deBase64.isPrivate()).isTrue();
    }

    @Test
    void recusaChaveInvalidaOuFraca() throws Exception {
        assertThatThrownBy(() -> JwtConfig.lerChave("nao-e-uma-chave"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_CHAVE_PRIVADA");
        String fraca = Base64.getEncoder().encodeToString(gerar(1024).getPrivate().getEncoded());
        assertThatThrownBy(() -> JwtConfig.lerChave(fraca)).isInstanceOf(IllegalArgumentException.class);
    }

    private static KeyPair gerar(int bits) throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(bits);
        return gerador.generateKeyPair();
    }
}
