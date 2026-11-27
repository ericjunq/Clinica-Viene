package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.MetodoAssinatura;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Assinatura coletada na tela (D-090).
 *
 * @param imagem DESENHO: PNG em data URI ({@code data:image/png;base64,...})
 * @param nomeDigitado NOME_DIGITADO: o nome completo de quem assina
 */
public record AssinaturaRequest(
        @NotNull MetodoAssinatura metodo, @Size(max = 400_000) String imagem, @Size(max = 150) String nomeDigitado) {

    @Override
    public String toString() {
        return "AssinaturaRequest[metodo=" + metodo + "]";
    }
}
