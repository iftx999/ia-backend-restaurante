package com.restoria.security;

/**
 * Lancada quando um token JWT esta ausente/invalido/expirado/adulterado.
 * A causa original do JJWT (io.jsonwebtoken.JwtException) nunca vaza para
 * fora de {@link JwtService} — ver metodo extrairEmail.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensagem) {
        super(mensagem);
    }

    public TokenInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
