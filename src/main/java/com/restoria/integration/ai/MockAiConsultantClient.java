package com.restoria.integration.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementacao falsa de {@link AiConsultantClient} para desenvolvimento local
 * sem gastar credito da API da Anthropic. Ativada com `restoria.ai.mock=true`
 * (ja definido no perfil Spring "mock" -> application-mock.yml).
 *
 * Nao chama nenhuma API externa: devolve uma resposta fixa, ecoando a ultima
 * pergunta do usuario, so para permitir testar o fluxo de ponta a ponta
 * (frontend -> backend -> persistencia -> resposta) sem depender de rede/chave.
 *
 * <p>{@code @Primary} pelo mesmo motivo de {@link AnthropicAiConsultantClient}:
 * default de injecao sem qualifier continua sendo "Claude" (aqui, sua versao
 * mock) mesmo com {@link MockOpenAiConsultantClient} tambem ativo em modo mock.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "restoria.ai", name = "mock", havingValue = "true")
public class MockAiConsultantClient implements AiConsultantClient {

    @Override
    public String enviarMensagem(String systemPrompt, List<AiMensagem> mensagens) {
        String ultimaPergunta = mensagens.isEmpty()
                ? ""
                : mensagens.get(mensagens.size() - 1).conteudo();

        return """
                [Resposta mockada - RESTORIA_AI_MOCK ativo, nenhuma chamada real foi feita a API da Anthropic]

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
            StringBuilder enviado = new StringBuilder();
            for (int i = 0; i < palavras.length; i++) {
                if (listener.cancelado()) {
                    listener.onCancelado(enviado.toString());
                    return;
                }
                String token = i < palavras.length - 1 ? palavras[i] + " " : palavras[i];
                enviado.append(token);
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
