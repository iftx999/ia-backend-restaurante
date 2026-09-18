package com.restoria.integration.ai;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementacao falsa de {@link AiConsultantClient} pro provedor GPT, mesma
 * ideia de {@link MockAiConsultantClient} (dev local sem gastar credito da
 * API da OpenAI). Ativada pela mesma flag `restoria.ai.mock=true` — ver
 * docs/05-multi-provedor-ia.md, secao 3.8.
 */
@Component
@Qualifier("gpt")
@ConditionalOnProperty(prefix = "restoria.ai", name = "mock", havingValue = "true")
public class MockOpenAiConsultantClient implements AiConsultantClient {

    @Override
    public String enviarMensagem(String systemPrompt, List<AiMensagem> mensagens) {
        String ultimaPergunta = mensagens.isEmpty()
                ? ""
                : mensagens.get(mensagens.size() - 1).conteudo();

        return """
                [Resposta mockada (GPT) - RESTORIA_AI_MOCK ativo, nenhuma chamada real foi feita a API da OpenAI]

                Voce perguntou: "%s"

                Aqui entraria a orientacao da IA sobre gestao do restaurante \
                (CMV, precificacao, estoque, compliance sanitario etc).\
                """.formatted(ultimaPergunta);
    }

    @Override
    public void enviarMensagemStream(String systemPrompt, List<AiMensagem> mensagens, RespostaIaStreamListener listener) {
        String textoCompleto = enviarMensagem(systemPrompt, mensagens);

        try {
            String[] palavras = textoCompleto.split(" ");
            for (int i = 0; i < palavras.length; i++) {
                String token = i < palavras.length - 1 ? palavras[i] + " " : palavras[i];
                listener.onToken(token);
                Thread.sleep(30);
            }
            listener.onConcluido(textoCompleto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            listener.onErro(e);
        } catch (Exception e) {
            listener.onErro(e);
        }
    }
}
