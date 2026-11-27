package br.com.clinicaviene.exceptions;

/** Horário já ocupado (profissional ou consultório) — vira HTTP 409. */
public class ConflitoAgendaException extends RuntimeException {

    public ConflitoAgendaException(String mensagem) {
        super(mensagem);
    }
}
