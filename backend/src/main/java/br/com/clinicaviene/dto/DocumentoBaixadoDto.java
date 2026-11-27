package br.com.clinicaviene.dto;

/** PDF decifrado e conferido pelo hash, pronto para o download. */
public record DocumentoBaixadoDto(String nomeArquivo, byte[] conteudo) {

    @Override
    public String toString() {
        return "DocumentoBaixadoDto[" + nomeArquivo + "]";
    }
}
