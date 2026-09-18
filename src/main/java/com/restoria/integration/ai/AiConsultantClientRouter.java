package com.restoria.integration.ai;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Escolhe qual {@link AiConsultantClient} usar por requisicao, de acordo com
 * o {@link ModeloIa} escolhido pelo usuario no chat consultivo (RF-22). Unico
 * ponto do codigo que conhece as duas implementacoes concretas — o resto do
 * fluxo ({@code ChatService}, persistencia, streaming SSE) so conhece a
 * interface {@link AiConsultantClient} e nao muda nada por causa disso.
 */
@Component
public class AiConsultantClientRouter {

    private final AiConsultantClient claudeClient;
    private final AiConsultantClient gptClient;

    public AiConsultantClientRouter(
            AiConsultantClient claudeClient,
            @Qualifier("gpt") AiConsultantClient gptClient) {
        this.claudeClient = claudeClient;
        this.gptClient = gptClient;
    }

    public AiConsultantClient resolver(ModeloIa modelo) {
        return modelo == ModeloIa.GPT ? gptClient : claudeClient;
    }
}
