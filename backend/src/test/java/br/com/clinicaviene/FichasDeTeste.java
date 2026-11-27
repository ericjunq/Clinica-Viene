package br.com.clinicaviene;

import br.com.clinicaviene.dto.DadosCadastroDto;
import br.com.clinicaviene.dto.DecisaoTermoRequest;
import br.com.clinicaviene.dto.EnderecoDto;
import br.com.clinicaviene.dto.FichaCadastroRequest;
import br.com.clinicaviene.dto.FormularioCadastroResponse;
import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.dto.RespostaAnamneseDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fichas de cadastro preenchidas para os testes do fluxo público (dados fictícios). */
public final class FichasDeTeste {

    private FichasDeTeste() {}

    /**
     * Responde todas as perguntas (alergia com detalhe, quando a pergunta for de alerta com detalhe), completa o
     * endereço, troca o telefone se informado, aceita a política e recusa os consentimentos opcionais.
     */
    public static FichaCadastroRequest completa(FormularioCadastroResponse formulario, String telefone) {
        Map<String, RespostaAnamneseDto> respostas = new HashMap<>();
        for (PerguntaAnamneseDto p : formulario.anamnese().perguntas()) {
            respostas.put(p.codigo(), switch (p.tipo()) {
                case SIM_NAO -> new RespostaAnamneseDto("NAO", null, null);
                case SIM_NAO_DETALHE -> p.ehAlertaClinico()
                        ? new RespostaAnamneseDto("SIM", null, "Dipirona")
                        : new RespostaAnamneseDto("NAO", null, null);
                case ESCOLHA_UNICA -> new RespostaAnamneseDto(p.opcoes().getFirst(), null, null);
                case MULTIPLA_ESCOLHA -> new RespostaAnamneseDto(null, List.of(p.opcoes().getFirst()), null);
                case DATA -> new RespostaAnamneseDto("2020-01-01", null, null);
                case NUMERO -> new RespostaAnamneseDto("1", null, null);
                case TEXTO_CURTO, TEXTO_LONGO -> new RespostaAnamneseDto("Nada a declarar", null, null);
            });
        }
        DadosCadastroDto d = formulario.dados();
        DadosCadastroDto dados = new DadosCadastroDto(
                d.nomeCompleto(), d.cpf(), d.dataNascimento(), d.email(), telefone == null ? d.telefone() : telefone,
                new EnderecoDto("45000-000", "Rua das Flores", "10", null, "Centro", "Vitória da Conquista", "BA"),
                d.responsavel());
        List<DecisaoTermoRequest> termos = formulario.termos().stream()
                .map(t -> new DecisaoTermoRequest(t.id(), t.obrigatorio()))
                .toList();
        return new FichaCadastroRequest(dados, formulario.anamnese().id(), respostas, termos, null);
    }
}
