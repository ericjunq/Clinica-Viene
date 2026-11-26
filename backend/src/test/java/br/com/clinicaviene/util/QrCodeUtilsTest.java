package br.com.clinicaviene.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class QrCodeUtilsTest {

    @Test
    void geraPngEmDataUri() {
        String dataUri = QrCodeUtils.pngDataUri("otpauth://totp/ClinicaViene:teste?secret=GEZDGNBVGY3TQOJQ");

        assertThat(dataUri).startsWith("data:image/png;base64,");
        byte[] png = Base64.getDecoder().decode(dataUri.substring("data:image/png;base64,".length()));
        assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }
}
