package com.restoria.assinatura;

public class LimiteUsoExcedidoException extends RuntimeException {

    public LimiteUsoExcedidoException(String mensagem) {
        super(mensagem);
    }
}
