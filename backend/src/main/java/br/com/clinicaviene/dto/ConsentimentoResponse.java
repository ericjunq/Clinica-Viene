package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.DecisaoTermo;
import br.com.clinicaviene.enums.OrigemAlteracao;
import br.com.clinicaviene.enums.TipoTermo;
import java.time.Instant;

/**
 * Situação atual de um termo para o paciente.
 *
 * @param situacao última decisão; {@code null} = nunca registrado
 */
public record ConsentimentoResponse(
        TipoTermo tipo,
        boolean obrigatorio,
        DecisaoTermo situacao,
        Integer versao,
        Instant registradoEm,
        OrigemAlteracao origem,
        boolean peloResponsavel) {}
