package com.restoria.assinatura;

/** Payload de webhook do Stripe com assinatura HMAC invalida (nao veio do Stripe de verdade). */
public class WebhookInvalidoException extends RuntimeException {

    public WebhookInvalidoException(Throwable causa) {
        super("Assinatura do webhook invalida", causa);
    }
}
