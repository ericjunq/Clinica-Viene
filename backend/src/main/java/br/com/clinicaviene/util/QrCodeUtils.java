package br.com.clinicaviene.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/** Gera QR codes em PNG (ZXing). */
public final class QrCodeUtils {

    private static final int TAMANHO_PIXELS = 240;

    private QrCodeUtils() {}

    /** PNG do QR code como {@code data:image/png;base64,...}, pronto para um {@code <img src>}. */
    public static String pngDataUri(String conteudo) {
        try {
            BitMatrix matriz = new QRCodeWriter()
                    .encode(conteudo, BarcodeFormat.QR_CODE, TAMANHO_PIXELS, TAMANHO_PIXELS, Map.of(EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", saida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Falha ao gerar QR code", e);
        }
    }
}
