package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.ModoConvite;

/**
 * Primeira tela do link: sem dados pessoais além do primeiro nome.
 *
 * @param precisaCodigo LINK: é preciso confirmar o código enviado ao e-mail; QUIOSQUE: não
 * @param emailMascarado para onde o código vai (LINK)
 */
public record ConviteAbertoResponse(ModoConvite modo, String primeiroNome, boolean precisaCodigo, String emailMascarado) {}
