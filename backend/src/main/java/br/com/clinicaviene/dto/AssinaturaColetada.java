package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.MetodoAssinatura;

/**
 * Assinatura conferida pelo provedor, pronta para entrar no documento.
 *
 * @param imagemDataUri DESENHO: PNG que vai impresso no PDF
 * @param nomeDigitado NOME_DIGITADO: nome que vai impresso no PDF
 * @param identificadorProvedor id da assinatura na plataforma externa (ZapSign), se houver
 */
public record AssinaturaColetada(
        MetodoAssinatura metodo, String imagemDataUri, String nomeDigitado, String identificadorProvedor) {

    @Override
    public String toString() {
        return "AssinaturaColetada[metodo=" + metodo + "]";
    }
}
