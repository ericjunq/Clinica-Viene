package br.com.clinicaviene.service;

import br.com.clinicaviene.dto.AssinaturaColetada;
import br.com.clinicaviene.dto.AssinaturaRequest;
import br.com.clinicaviene.enums.MetodoAssinatura;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/**
 * Provedor interno (D-090): assinatura desenhada na tela (PNG) ou nome digitado, impressa no PDF junto da trilha de
 * evidências. A identidade já foi confirmada antes (código no e-mail ou documento conferido no quiosque — D-100).
 */
@Service
public class ProvedorAssinaturaInterno implements ProvedorAssinatura {

    static final String PREFIXO_PNG = "data:image/png;base64,";
    private static final int TAMANHO_MAXIMO_BYTES = 250_000;
    private static final int PIXELS_MINIMOS_DESENHADOS = 150;
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @Override
    public String nome() {
        return "INTERNO";
    }

    @Override
    public AssinaturaColetada coletar(AssinaturaRequest pedido, String nomeSignatario) {
        if (pedido.metodo() == MetodoAssinatura.DESENHO) {
            return new AssinaturaColetada(MetodoAssinatura.DESENHO, conferirDesenho(pedido.imagem()), null, null);
        }
        String digitado = pedido.nomeDigitado() == null ? "" : pedido.nomeDigitado().trim().replaceAll("\\s+", " ");
        if (!normalizar(digitado).equals(normalizar(nomeSignatario))) {
            throw new RegraNegocioException("Para assinar digitando, escreva o nome completo exatamente como no cadastro: "
                    + nomeSignatario + ".");
        }
        return new AssinaturaColetada(MetodoAssinatura.NOME_DIGITADO, null, digitado, null);
    }

    private static String conferirDesenho(String imagem) {
        if (imagem == null || !imagem.startsWith(PREFIXO_PNG)) {
            throw new RegraNegocioException("Assinatura desenhada inválida. Desenhe de novo.");
        }
        byte[] png;
        try {
            png = Base64.getDecoder().decode(imagem.substring(PREFIXO_PNG.length()));
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Assinatura desenhada inválida. Desenhe de novo.");
        }
        if (png.length > TAMANHO_MAXIMO_BYTES || png.length < ASSINATURA_PNG.length || !comecaComPng(png)) {
            throw new RegraNegocioException("Assinatura desenhada inválida. Desenhe de novo.");
        }
        BufferedImage desenho;
        try {
            desenho = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            desenho = null;
        }
        if (desenho == null || pixelsDesenhados(desenho) < PIXELS_MINIMOS_DESENHADOS) {
            throw new RegraNegocioException("A assinatura está em branco ou muito pequena. Desenhe de novo.");
        }
        return imagem;
    }

    /** Pixels visíveis e escuros (o fundo do quadro é transparente ou branco). */
    private static int pixelsDesenhados(BufferedImage imagem) {
        int total = 0;
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                int argb = imagem.getRGB(x, y);
                int alfa = (argb >>> 24) & 0xFF;
                int brilho = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
                if (alfa > 64 && brilho < 200) {
                    total++;
                }
            }
        }
        return total;
    }

    private static boolean comecaComPng(byte[] dados) {
        for (int i = 0; i < ASSINATURA_PNG.length; i++) {
            if (dados[i] != ASSINATURA_PNG[i]) {
                return false;
            }
        }
        return true;
    }

    static String normalizar(String nome) {
        return Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
