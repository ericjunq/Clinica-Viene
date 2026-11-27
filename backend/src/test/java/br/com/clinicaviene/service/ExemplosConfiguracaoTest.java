package br.com.clinicaviene.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.clinicaviene.dto.PerguntaAnamneseDto;
import br.com.clinicaviene.enums.TipoTermo;
import br.com.clinicaviene.validation.ModeloAnamneseValidator;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Os exemplos que vêm com o sistema (D-089) existem e passam nas mesmas validações do MASTER. */
class ExemplosConfiguracaoTest {

    @Test
    void haUmTextoDeExemploComTituloParaCadaTipoDeTermo() {
        for (TipoTermo tipo : TipoTermo.values()) {
            String texto = TermoService.lerExemplo("exemplos/termos/" + tipo.name().toLowerCase(Locale.ROOT) + ".md");
            assertThat(texto).startsWith("# ").contains("RASCUNHO DE EXEMPLO");
        }
    }

    @Test
    void exemploDeAnamneseOdontologicaEValidoECompleto() {
        JsonMapper json = JsonMapper.builder().build();
        JsonNode exemplo = json.readTree(TermoService.lerExemplo("exemplos/anamnese/odontologia.json"));
        List<PerguntaAnamneseDto> perguntas = json.convertValue(exemplo.get("perguntas"), new TypeReference<List<PerguntaAnamneseDto>>() {});

        assertThatCode(() -> ModeloAnamneseValidator.validar(perguntas)).doesNotThrowAnyException();
        assertThat(perguntas).hasSizeGreaterThanOrEqualTo(40);
        assertThat(perguntas)
                .extracting(PerguntaAnamneseDto::codigo)
                .contains("alergia_medicamento", "alergia_anestesico", "anticoagulante", "bisfosfonato", "gravidez");
        assertThat(perguntas).filteredOn(PerguntaAnamneseDto::ehAlertaClinico).hasSizeGreaterThanOrEqualTo(10);
    }
}
