package com.restoria.integration.ai;

/**
 * Erro ao chamar a API da Anthropic (falha de rede, timeout, resposta invalida, etc).
 */
public class AiConsultantException extends RuntimeException {

    public AiConsultantException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiConsultantException(String message) {
        super(message);
    }
}
