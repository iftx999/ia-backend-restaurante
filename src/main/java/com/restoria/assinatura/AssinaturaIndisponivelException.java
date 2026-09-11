package com.restoria.assinatura;

/** Falha de comunicacao com o Stripe ao criar a sessao de checkout. */
public class AssinaturaIndisponivelException extends RuntimeException {

    public AssinaturaIndisponivelException(Throwable causa) {
        super("Nao foi possivel iniciar o checkout agora. Tente novamente em instantes.", causa);
    }
}
