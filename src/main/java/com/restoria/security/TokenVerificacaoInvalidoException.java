package com.restoria.security;

public class TokenVerificacaoInvalidoException extends RuntimeException {

    public TokenVerificacaoInvalidoException() {
        super("Link de verificacao invalido ou expirado. Peca um novo dentro do app.");
    }
}
