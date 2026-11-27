package br.com.clinicaviene.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Ficha assinada e guardada.
 *
 * @param sha256 hash do PDF assinado (prova de integridade)
 * @param entregaEnviadaPara e-mail (mascarado) que recebeu o link para baixar o PDF, se houver
 */
public record FichaAssinadaResponse(
        UUID fichaId, UUID documentoId, String sha256, Instant assinadoEm, String entregaEnviadaPara) {}
