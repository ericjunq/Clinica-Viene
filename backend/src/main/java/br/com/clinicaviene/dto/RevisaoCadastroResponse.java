package br.com.clinicaviene.dto;

/**
 * Ficha conferida e pronta para a assinatura (2F).
 *
 * @param peloResponsavel paciente menor: quem assina é o responsável legal
 */
public record RevisaoCadastroResponse(boolean prontoParaAssinar, boolean peloResponsavel) {}
