package com.restoria.security;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException(String email) {
        super("Ja existe um usuario cadastrado com o email: " + email);
    }
}
