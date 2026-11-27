package br.com.clinicaviene.service;

import br.com.clinicaviene.dto.AssinaturaColetada;
import br.com.clinicaviene.dto.AssinaturaRequest;

/**
 * Ponto de troca da plataforma de assinatura (D-029, D-090): hoje o provedor interno; o adaptador ZapSign entra
 * atrás desta interface quando houver conta/sandbox.
 */
public interface ProvedorAssinatura {

    /** Nome gravado na assinatura (ex.: {@code INTERNO}, {@code ZAPSIGN}). */
    String nome();

    /**
     * Confere a assinatura que o signatário fez e a prepara para entrar no documento.
     *
     * @param nomeSignatario nome completo de quem assina (paciente ou responsável legal)
     */
    AssinaturaColetada coletar(AssinaturaRequest pedido, String nomeSignatario);
}
