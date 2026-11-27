package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.clinicaviene.dto.AssinaturaColetada;
import br.com.clinicaviene.dto.AssinaturaRequest;
import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ProvedorAssinaturaInternoTest {

    private final ProvedorAssinaturaInterno provedor = new ProvedorAssinaturaInterno();

    @Test
    void aceitaAssinaturaDesenhada() {
        String png = AssinaturasDeTeste.desenhada();
        AssinaturaColetada coletada = provedor.coletar(new AssinaturaRequest(MetodoAssinatura.DESENHO, png, null), "Ana");
        assertThat(coletada.metodo()).isEqualTo(MetodoAssinatura.DESENHO);
        assertThat(coletada.imagemDataUri()).isEqualTo(png);
        assertThat(provedor.nome()).isEqualTo("INTERNO");
    }

    @Test
    void recusaDesenhoEmBrancoOuQueNaoEPng() {
        assertRecusa(new AssinaturaRequest(MetodoAssinatura.DESENHO, AssinaturasDeTeste.emBranco(), null));
        assertRecusa(new AssinaturaRequest(MetodoAssinatura.DESENHO, null, null));
        assertRecusa(new AssinaturaRequest(MetodoAssinatura.DESENHO, "data:image/png;base64,@@@", null));
        assertRecusa(new AssinaturaRequest(
                MetodoAssinatura.DESENHO,
                "data:image/png;base64," + Base64.getEncoder().encodeToString("não é png".getBytes()),
                null));
        assertRecusa(new AssinaturaRequest(MetodoAssinatura.DESENHO, "data:image/jpeg;base64,AAAA", null));
    }

    @Test
    void nomeDigitadoPrecisaSerONomeDoSignatarioSemDiferencaDeAcentoOuMaiusculas() {
        AssinaturaColetada coletada = provedor.coletar(
                new AssinaturaRequest(MetodoAssinatura.NOME_DIGITADO, null, "  maria  JOSE da conceicao "),
                "Maria José da Conceição");
        assertThat(coletada.nomeDigitado()).isEqualTo("maria JOSE da conceicao");

        assertThatThrownBy(() -> provedor.coletar(
                        new AssinaturaRequest(MetodoAssinatura.NOME_DIGITADO, null, "Maria"), "Maria José da Conceição"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Maria José da Conceição");
        assertThatThrownBy(() -> provedor.coletar(
                        new AssinaturaRequest(MetodoAssinatura.NOME_DIGITADO, null, null), "Maria"))
                .isInstanceOf(RegraNegocioException.class);
    }

    private void assertRecusa(AssinaturaRequest pedido) {
        assertThatThrownBy(() -> provedor.coletar(pedido, "Ana")).isInstanceOf(RegraNegocioException.class);
    }

    /** PNGs gerados na hora para os testes. */
    public static final class AssinaturasDeTeste {

        private AssinaturasDeTeste() {}

        public static String desenhada() {
            BufferedImage imagem = new BufferedImage(400, 150, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = imagem.createGraphics();
            g.setColor(new Color(30, 20, 10));
            g.setStroke(new BasicStroke(4));
            g.drawLine(20, 100, 120, 30);
            g.drawLine(120, 30, 220, 110);
            g.drawLine(220, 110, 380, 40);
            g.dispose();
            return dataUri(imagem);
        }

        public static String emBranco() {
            return dataUri(new BufferedImage(400, 150, BufferedImage.TYPE_INT_ARGB));
        }

        private static String dataUri(BufferedImage imagem) {
            try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
                javax.imageio.ImageIO.write(imagem, "png", saida);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
