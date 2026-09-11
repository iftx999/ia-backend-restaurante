package com.restoria.integration.ai;

/**
 * Uma mensagem no formato aceito pela API da Anthropic (role "user"/"assistant").
 */
public record AiMensagem(String role, String conteudo) {

    public static AiMensagem doUsuario(String conteudo) {
        return new AiMensagem("user", conteudo);
    }

    public static AiMensagem daIa(String conteudo) {
        return new AiMensagem("assistant", conteudo);
    }
}
