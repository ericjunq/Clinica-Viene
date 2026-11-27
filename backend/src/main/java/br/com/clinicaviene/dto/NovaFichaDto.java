package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.ModeloAnamnese;
import br.com.clinicaviene.entity.TermoVersao;
import br.com.clinicaviene.enums.OrigemAlteracao;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tudo que entra na ficha assinada (uso interno, entre o fluxo público e o {@code FichaAnamneseService}).
 *
 * @param dados dados cadastrais já validados e normalizados
 * @param aceitos ids das versões de termo marcadas
 * @param identidade como a identidade foi confirmada (texto que vai na trilha de evidências do PDF)
 * @param evidencias evidências técnicas (IP, aparelho, sessão, convite…) — gravadas cifradas
 */
public record NovaFichaDto(
        UUID pacienteId,
        UUID conviteId,
        OrigemAlteracao origem,
        DadosCadastroDto dados,
        ModeloAnamnese modelo,
        List<PerguntaAnamneseDto> perguntas,
        Map<String, RespostaAnamneseDto> respostas,
        List<String> alertas,
        List<TermoVersao> termos,
        Set<UUID> aceitos,
        AcompanhanteDto acompanhante,
        boolean peloResponsavel,
        String provedor,
        AssinaturaColetada assinatura,
        String identidade,
        Map<String, Object> evidencias,
        Instant assinadoEm) {

    @Override
    public String toString() {
        return "NovaFichaDto[pacienteId=" + pacienteId + "]";
    }
}
