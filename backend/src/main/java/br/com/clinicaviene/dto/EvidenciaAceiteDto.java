package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.OrigemAlteracao;
import java.util.UUID;

/**
 * Evidências de uma decisão sobre termos (D-093): por onde, se foi o responsável legal, IP, aparelho e — quando
 * registrado na recepção — quem registrou.
 */
public record EvidenciaAceiteDto(
        OrigemAlteracao origem, boolean peloResponsavel, String ip, String dispositivo, UUID registradoPor) {}
