package com.restoria.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementacao falsa de {@link EmailSender} para desenvolvimento local sem
 * credencial SMTP configurada. Ativada com `restoria.email.mock=true`
 * (default enquanto MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD nao forem
 * definidos — ver docs/04-roadmap.md).
 *
 * Nao envia nada de verdade: so loga o link de verificacao, permitindo
 * testar o fluxo de cadastro -> confirmacao de ponta a ponta em dev.
 */
@Component
@ConditionalOnProperty(prefix = "restoria.email", name = "mock", havingValue = "true", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

    @Override
    public void enviarVerificacaoEmail(String destinatario, String nomeUsuario, String linkVerificacao) {
        log.info(
                "[RESTORIA_EMAIL_MOCK] Nenhum e-mail real foi enviado. Destinatario: {} | Nome: {} | Link de verificacao: {}",
                destinatario, nomeUsuario, linkVerificacao);
    }
}
