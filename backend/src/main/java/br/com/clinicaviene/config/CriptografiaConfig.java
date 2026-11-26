package br.com.clinicaviene.config;

import br.com.clinicaviene.util.CifradorAesGcm;
import br.com.clinicaviene.util.CpfHasher;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CriptografiaProperties.class)
public class CriptografiaConfig {

    @Bean
    CifradorAesGcm cifrador(CriptografiaProperties propriedades) {
        return new CifradorAesGcm(lerChaves(propriedades.chaves()), propriedades.chaveAtiva().trim());
    }

    @Bean
    CpfHasher hashCpf(CriptografiaProperties propriedades) {
        return new CpfHasher(decodificar(propriedades.cpfHmacChave().trim(), "CPF_HMAC_CHAVE"));
    }

    /** Lê {@code v1:BASE64,v2:BASE64} em um mapa versão → bytes da chave. */
    static Map<String, byte[]> lerChaves(String configuracao) {
        Map<String, byte[]> chaves = new LinkedHashMap<>();
        for (String item : configuracao.split(",")) {
            String texto = item.trim();
            if (texto.isEmpty()) {
                continue;
            }
            int posicao = texto.indexOf(':');
            if (posicao <= 0) {
                throw new IllegalArgumentException("CRIPTO_CHAVES deve estar no formato v1:BASE64,v2:BASE64");
            }
            String versao = texto.substring(0, posicao).trim();
            if (chaves.containsKey(versao)) {
                throw new IllegalArgumentException("Versão de chave repetida em CRIPTO_CHAVES: '" + versao + "'");
            }
            chaves.put(versao, decodificar(texto.substring(posicao + 1).trim(), "CRIPTO_CHAVES (" + versao + ")"));
        }
        return chaves;
    }

    private static byte[] decodificar(String base64, String origem) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(origem + " não está em Base64 válido", e);
        }
    }
}
