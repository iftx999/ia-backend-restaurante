package com.restoria.security;

/**
 * Lancada quando se tenta resolver o usuario autenticado da requisicao atual
 * mas nao ha nenhum (ou o usuario do token nao existe mais no banco). Nao
 * deveria acontecer em condicoes normais: o JwtAuthenticationFilter so
 * autentica requisicoes com um token valido de um usuario existente, e o
 * SecurityConfig bloqueia rotas protegidas sem autenticacao.
 */
public class UsuarioNaoAutenticadoException extends RuntimeException {

    public UsuarioNaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
