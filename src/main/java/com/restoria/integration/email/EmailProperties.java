package com.restoria.integration.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.email` do application.yml. As credenciais SMTP em
 * si (host/usuario/senha) vivem em `spring.mail.*`, configuraveis via
 * MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD — nunca hardcoded (ver
 * CLAUDE.md).
 *
 * @param mock quando true (default, ate uma credencial SMTP real ser
 *             configurada), usa {@link MockEmailSender} em vez de enviar
 *             e-mail de verdade.
 * @param remetente endereco "From" usado no e-mail de verificacao.
 * @param linkBase URL base do frontend usada para montar o link de
 *                  verificacao (ex: "https://app.restoria.com" ->
 *                  "{linkBase}/verificar-email?token=...").
 */
@ConfigurationProperties(prefix = "restoria.email")
public record EmailProperties(
        @DefaultValue("true") boolean mock,
        String remetente,
        String linkBase
) {
}
