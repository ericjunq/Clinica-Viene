package br.com.clinicaviene.dto;

import br.com.clinicaviene.entity.ConviteCadastro;
import br.com.clinicaviene.enums.AreaAtuacao;
import br.com.clinicaviene.enums.ModoConvite;
import br.com.clinicaviene.enums.SituacaoConvite;
import java.time.Instant;
import java.util.UUID;

/** Convite na lista do paciente (sem o token). */
public record ConviteResponse(
        UUID id,
        ModoConvite modo,
        AreaAtuacao areaAtuacao,
        SituacaoConvite situacao,
        Instant criadoEm,
        Instant expiraEm,
        Instant enviadoPorEmailEm,
        Instant usadoEm) {

    public static ConviteResponse from(ConviteCadastro c, Instant agora) {
        return new ConviteResponse(
                c.getId(), c.getModo(), c.getAreaAtuacao(), c.situacao(agora), c.getCriadoEm(), c.getExpiraEm(), c.getEnviadoPorEmailEm(),
                c.getUsadoEm());
    }
}
