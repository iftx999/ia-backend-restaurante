package com.restoria.integration.ai;

/**
 * Erro ao chamar a API de embeddings da Voyage AI (falha de rede, timeout, resposta invalida, etc).
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingException(String message) {
        super(message);
    }
}
