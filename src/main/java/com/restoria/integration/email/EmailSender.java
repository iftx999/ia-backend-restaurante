package com.restoria.integration.email;

/**
 * Unico ponto de contato para envio de e-mail. Nenhum outro Service deve
 * falar com um provedor de e-mail diretamente (mesma regra do
 * {@code AiConsultantClient}, ver CLAUDE.md).
 *
 * Duas implementacoes: {@link SmtpEmailSender} (producao, via SMTP) e
 * {@link MockEmailSender} (dev local, ativada via `restoria.email.mock=true`
 * — default enquanto nenhuma credencial de SMTP foi configurada).
 */
public interface EmailSender {

    /**
     * Envia o e-mail de confirmacao de cadastro (RF pendente, ver
     * docs/04-roadmap.md). Nao deve lancar excecao de forma que quebre o
     * fluxo de registro do usuario — falhas de envio sao responsabilidade de
     * quem chama tratar (ver {@code AuthService.registrar}).
     */
    void enviarVerificacaoEmail(String destinatario, String nomeUsuario, String linkVerificacao);
}
