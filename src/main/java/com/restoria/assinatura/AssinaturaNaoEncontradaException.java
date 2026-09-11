package com.restoria.assinatura;

/** Evento de webhook referenciando um stripeCustomerId sem Assinatura correspondente no banco. */
public class AssinaturaNaoEncontradaException extends RuntimeException {

    public AssinaturaNaoEncontradaException(String stripeCustomerId) {
        super("Nenhuma assinatura encontrada para o customer do Stripe: " + stripeCustomerId);
    }
}
