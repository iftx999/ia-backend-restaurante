package com.restoria.integration.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Implementacao real de {@link EmailSender}: envia via SMTP usando o
 * {@link JavaMailSender} auto-configurado pelo Spring Boot a partir de
 * `spring.mail.*` (MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD). Ativa
 * por padrao apenas quando `restoria.email.mock=false` (ver
 * {@link MockEmailSender} para o default).
 */
@Component
@ConditionalOnProperty(prefix = "restoria.email", name = "mock", havingValue = "false")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void enviarVerificacaoEmail(String destinatario, String nomeUsuario, String linkVerificacao) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(properties.remetente());
            helper.setTo(destinatario);
            helper.setSubject("Confirme seu e-mail — RestorIA");
            helper.setText(corpoHtml(nomeUsuario, linkVerificacao), true);
            mailSender.send(mensagem);
        } catch (MessagingException | RuntimeException e) {
            // Nao propaga: falha de envio nao deve derrubar o cadastro do
            // usuario (ver AuthService.registrar). So loga pra investigar.
            log.warn("Falha ao enviar e-mail de verificacao para {}: {}", destinatario, e.getMessage());
        }
    }

    private String corpoHtml(String nomeUsuario, String linkVerificacao) {
        return """
                <p>Oi, %s!</p>
                <p>Confirme seu e-mail pra ativar sua conta na RestorIA:</p>
                <p><a href="%s">Confirmar e-mail</a></p>
                <p>Se você não criou essa conta, pode ignorar esta mensagem.</p>
                """.formatted(nomeUsuario, linkVerificacao);
    }
}
