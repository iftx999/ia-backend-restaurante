package com.restoria.security;

public class ReenvioVerificacaoMuitoRapidoException extends RuntimeException {

    public ReenvioVerificacaoMuitoRapidoException() {
        super("Aguarde um minuto antes de pedir outro e-mail de verificacao.");
    }
}
