package com.restoria.chat;

public class ConversaNaoEncontradaException extends RuntimeException {

    public ConversaNaoEncontradaException(Long conversaId) {
        super("Conversa nao encontrada: " + conversaId);
    }

    public ConversaNaoEncontradaException(String conversationId) {
        super("conversationId invalido: " + conversationId);
    }
}
