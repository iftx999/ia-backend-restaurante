package com.restoria.integration.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiConsultantClientRouterTest {

    private final AiConsultantClient claudeClient = new AiConsultantClientFake("claude");
    private final AiConsultantClient gptClient = new AiConsultantClientFake("gpt");
    private final AiConsultantClientRouter router = new AiConsultantClientRouter(claudeClient, gptClient);

    @Test
    void resolveClaudeParaModeloClaude() {
        assertThat(router.resolver(ModeloIa.CLAUDE)).isSameAs(claudeClient);
    }

    @Test
    void resolveGptParaModeloGpt() {
        assertThat(router.resolver(ModeloIa.GPT)).isSameAs(gptClient);
    }

    private static class AiConsultantClientFake implements AiConsultantClient {
        private final String nome;

        AiConsultantClientFake(String nome) {
            this.nome = nome;
        }

        @Override
        public String enviarMensagem(String systemPrompt, List<AiMensagem> mensagens) {
            return nome;
        }

        @Override
        public void enviarMensagemStream(String systemPrompt, List<AiMensagem> mensagens, RespostaIaStreamListener listener) {
            listener.onConcluido(nome);
        }
    }
}
