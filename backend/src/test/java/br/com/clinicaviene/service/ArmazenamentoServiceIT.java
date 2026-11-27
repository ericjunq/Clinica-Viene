package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.TestcontainersConfiguration;
import br.com.clinicaviene.config.ArmazenamentoProperties;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** Storage S3 (SeaweedFS em contêiner): o arquivo sai cifrado do servidor e volta legível. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ArmazenamentoServiceIT {

    @Autowired
    private ArmazenamentoService armazenamentoService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private ArmazenamentoProperties propriedades;

    @Test
    void guardaCifradoELeDeVolta() {
        String chave = "teste/" + UUID.randomUUID() + ".pdf";
        byte[] pdf = "%PDF-1.7 ficha de teste com acentuação".getBytes(StandardCharsets.UTF_8);

        armazenamentoService.guardar(chave, pdf);

        byte[] noStorage = s3Client.getObjectAsBytes(r -> r.bucket(propriedades.bucket()).key(chave)).asByteArray();
        assertThat(noStorage).isNotEqualTo(pdf);
        assertThat(new String(noStorage, StandardCharsets.ISO_8859_1)).doesNotContain("PDF");
        assertThat(armazenamentoService.ler(chave)).isEqualTo(pdf);
    }

    @Test
    void chaveInexistente() {
        assertThatThrownBy(() -> armazenamentoService.ler("nao/existe.pdf")).isInstanceOf(NoSuchKeyException.class);
    }
}
