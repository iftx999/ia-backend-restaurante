package com.restoria.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Implementacao de {@link AiConsultantClient} que chama a API de Responses da
 * OpenAI (`POST /v1/responses`) — alternativa ao Claude escolhida manualmente
 * pelo usuario no chat (RF-22, ver docs/05-multi-provedor-ia.md). Ativa por
 * padrao; desativada quando `restoria.ai.mock=true` (mesma flag do Claude,
 * ver {@link MockOpenAiConsultantClient}).
 *
 * <p>{@code @Qualifier("gpt")}: sem isso, injetar {@link AiConsultantClient}
 * sem qualifier seria ambiguo (duas implementacoes ativas ao mesmo tempo) —
 * ver {@code @Primary} em {@link AnthropicAiConsultantClient}.
 */
@Component
@Qualifier("gpt")
@ConditionalOnProperty(prefix = "restoria.ai", name = "mock", havingValue = "false", matchIfMissing = true)
public class OpenAiConsultantClient implements AiConsultantClient {

    private final RestClient restClient;
    private final ControleChamadaIa controleChamada;
    private final OpenAiProperties properties;
    private final OpenAiSseStreamProcessor sseStreamProcessor = new OpenAiSseStreamProcessor();

    public OpenAiConsultantClient(RestClient openAiRestClient, OpenAiProperties properties) {
        this.restClient = openAiRestClient;
        this.properties = properties;
        this.controleChamada = ControleChamadaIa.padrao("a API da OpenAI", properties.maxConcorrencia());
    }

    @Override
    public String enviarMensagem(String systemPrompt, List<AiMensagem> mensagens) {
        ObjectNode corpo = montarCorpoRequisicao(systemPrompt, mensagens);

        try {
            JsonNode resposta = controleChamada.executar(() -> restClient.post()
                    .uri("/v1/responses")
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class));

            return extrairTexto(resposta);
        } catch (RestClientException e) {
            throw new AiConsultantException("Falha ao chamar a API da OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public void enviarMensagemStream(String systemPrompt, List<AiMensagem> mensagens, RespostaIaStreamListener listener) {
        ObjectNode corpo = montarCorpoRequisicao(systemPrompt, mensagens);
        corpo.put("stream", true);

        try {
            controleChamada.executar(() -> restClient.post()
                    .uri("/v1/responses")
                    .header("accept", "text/event-stream")
                    .body(corpo)
                    .exchange((request, response) -> {
                        ControleChamadaIa.lancarSeErro(response);
                        try (InputStream corpoResposta = response.getBody();
                             Reader reader = new InputStreamReader(corpoResposta, StandardCharsets.UTF_8)) {
                            sseStreamProcessor.processar(reader, listener);
                        } catch (IOException e) {
                            listener.onErro(new AiConsultantException(
                                    "Falha ao ler o stream de resposta da OpenAI: " + e.getMessage(), e));
                        }
                        return null;
                    }));
        } catch (RestClientException e) {
            listener.onErro(new AiConsultantException("Falha ao chamar a API da OpenAI (stream): " + e.getMessage(), e));        } catch (IaSobrecarregadaException e) {
            listener.onErro(e);
        }
    }

    private ObjectNode montarCorpoRequisicao(String systemPrompt, List<AiMensagem> mensagens) {
        ObjectNode corpo = JsonNodeFactory.instance.objectNode();
        corpo.put("model", properties.model());
        corpo.put("max_output_tokens", properties.maxTokens());
        corpo.put("instructions", systemPrompt);

        ArrayNode input = corpo.putArray("input");
        for (AiMensagem mensagem : mensagens) {
            ObjectNode item = input.addObject();
            item.put("type", "message");
            item.put("role", mensagem.role());

            if (mensagem.temImagem()) {
                ArrayNode blocos = item.putArray("content");

                ObjectNode blocoImagem = blocos.addObject();
                blocoImagem.put("type", "input_image");
                blocoImagem.put("image_url", "data:" + mensagem.imagemMediaType() + ";base64," + mensagem.imagemBase64());

                ObjectNode blocoTexto = blocos.addObject();
                blocoTexto.put("type", "input_text");
                blocoTexto.put("text", mensagem.conteudo());
            } else {
                item.put("content", mensagem.conteudo());
            }
        }

        // Ferramenta nativa da OpenAI (server-side, sem infra propria) — paridade
        // com a busca na web ja usada no client da Anthropic (mesma justificativa:
        // perguntas com informacao local/atual, ex: "fornecedores em Limeira").
        ArrayNode ferramentas = corpo.putArray("tools");
        ObjectNode webSearch = ferramentas.addObject();
        webSearch.put("type", "web_search");

        return corpo;
    }

    private String extrairTexto(JsonNode resposta) {
        if (resposta == null) {
            throw new AiConsultantException("Resposta vazia da API da OpenAI");
        }

        JsonNode output = resposta.path("output");
        if (!output.isArray() || output.isEmpty()) {
            throw new AiConsultantException("Resposta da API da OpenAI sem conteudo: " + resposta);
        }

        StringBuilder texto = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            for (JsonNode bloco : item.path("content")) {
                if ("output_text".equals(bloco.path("type").asText())) {
                    texto.append(bloco.path("text").asText());
                }
            }
        }

        if (texto.isEmpty()) {
            throw new AiConsultantException("Resposta da API da OpenAI sem bloco de texto: " + resposta);
        }

        return texto.toString();
    }
}
