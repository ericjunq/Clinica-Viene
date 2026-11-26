package br.com.clinicaviene.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Assinatura e validação do JWT de acesso com RSA (RS256). A chave vem do ambiente (D-071). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class JwtConfig {

    public static final String ID_CHAVE = "v1";

    @Bean
    RSAKey chaveJwt(AuthProperties propriedades) {
        return lerChave(propriedades.chavePrivadaJwt());
    }

    @Bean
    JwtEncoder codificadorJwt(RSAKey chave) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(chave)));
    }

    @Bean
    JwtDecoder decodificadorJwt(RSAKey chave) throws Exception {
        NimbusJwtDecoder decodificador =
                NimbusJwtDecoder.withPublicKey(chave.toRSAPublicKey()).build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(AuthProperties.EMISSOR));
        return decodificador;
    }

    /** Lê a chave privada (Base64 de PKCS#8 DER, ou PEM) e deriva a pública. */
    static RSAKey lerChave(String texto) {
        String base64 = texto.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privada = (RSAPrivateCrtKey) fabrica.generatePrivate(new PKCS8EncodedKeySpec(der));
            if (privada.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("JWT_CHAVE_PRIVADA precisa ter pelo menos 2048 bits");
            }
            RSAPublicKey publica = (RSAPublicKey)
                    fabrica.generatePublic(new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            return new RSAKey.Builder(publica).privateKey(privada).keyID(ID_CHAVE).build();
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalArgumentException(
                    "JWT_CHAVE_PRIVADA inválida (esperado RSA PKCS#8 em Base64): " + e.getClass().getSimpleName(), e);
        }
    }
}
