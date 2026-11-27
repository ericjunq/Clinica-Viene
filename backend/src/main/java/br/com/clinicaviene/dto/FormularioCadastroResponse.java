package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.TipoTermo;
import java.util.List;
import java.util.UUID;

/**
 * Tudo que o paciente precisa para preencher: o pré-cadastro (para conferir), o modelo de anamnese vigente e os
 * termos vigentes.
 *
 * @param menorDeIdade quem preenche e assina é o responsável legal (LGPD art. 14)
 */
public record FormularioCadastroResponse(
        ModoConvite modo, boolean menorDeIdade, DadosCadastroDto dados, Anamnese anamnese, List<Termo> termos) {

    public record Anamnese(UUID id, String titulo, int versao, List<PerguntaAnamneseDto> perguntas) {}

    public record Termo(UUID id, TipoTermo tipo, boolean obrigatorio, int versao, String titulo, String texto) {}

    @Override
    public String toString() {
        return "FormularioCadastroResponse[modo=" + modo + "]";
    }
}
