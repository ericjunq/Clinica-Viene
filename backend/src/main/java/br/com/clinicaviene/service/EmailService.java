package br.com.clinicaviene.service;

import br.com.clinicaviene.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mails do sistema. Regra (CONTEXTO §10, D-026): o corpo nunca contém dado clínico.
 *
 * <p>Falha de envio é registrada no log (sem o destinatário nem o conteúdo) e não interrompe a operação: o
 * usuário pode pedir o reenvio.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public void enviar(String para, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(emailProperties.remetente());
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.warn("Falha ao enviar e-mail ({})", e.getClass().getSimpleName());
        }
    }
}
