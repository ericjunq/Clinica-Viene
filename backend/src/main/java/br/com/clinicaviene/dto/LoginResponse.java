package br.com.clinicaviene.dto;

import br.com.clinicaviene.enums.EtapaLogin;
import java.time.Instant;

/**
 * Senha aceita: devolve o desafio (token de uso único) e a próxima etapa (cadastrar ou verificar o 2FA).
 */
public record LoginResponse(String desafio, EtapaLogin etapa, Instant expiraEm) {}
