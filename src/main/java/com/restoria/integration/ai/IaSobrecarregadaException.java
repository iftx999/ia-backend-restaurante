package com.restoria.integration.ai;

/**
 * Todas as vagas de chamada simultanea ao provedor de IA estao ocupadas ha mais
 * tempo do que {@link ControleChamadaIa} aceita esperar. Vira HTTP 503 para o
 * cliente (ver {@code GlobalExceptionHandler}) — melhor recusar rapido do que
 * empilhar requisicoes ate o provedor devolver 429 para todo mundo.
 */
public class IaSobrecarregadaException extends RuntimeException {

    public IaSobrecarregadaException(String provedor) {
        super("Muitas requisicoes simultaneas a " + provedor + " no momento. Tente novamente em instantes.");
    }
}
