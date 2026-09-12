package com.restoria.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Implementacao real de {@link AiConsultantClient}: chama a API da Anthropic
 * (endpoint /v1/messages). Ativa por padrao; desativada quando
 * `restoria.ai.mock=true` (ver {@link MockAiConsultantClient}).
 */
@Component
@ConditionalOnProperty(prefix = "restoria.ai", name = "mock", havingValue = "false", matchIfMissing = true)
public class AnthropicAiConsultantClient implements AiConsultantClient {

    private final RestClient restClient;
    private final AiProperties properties;
    private final AnthropicSseStreamProcessor sseStreamProcessor = new AnthropicSseStreamProcessor();

    public AnthropicAiConsultantClient(RestClient anthropicRestClient, AiProperties properties) {
        this.restClient = anthropicRestClient;
        this.properties = properties;
    }

    @Override
    public String enviarMensagem(String systemPrompt, List<AiMensagem> mensagens) {
        ObjectNode corpo = montarCorpoRequisicao(systemPrompt, mensagens);

        try {
            JsonNode resposta = restClient.post()
                    .uri("/v1/messages")
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);

            return extrairTexto(resposta);
        } catch (RestClientException e) {
            throw new AiConsultantException("Falha ao chamar a API da Anthropic: " + e.getMessage(), e);
        }
    }

    @Override
    public void enviarMensagemStream(String systemPrompt, List<AiMensagem> mensagens, RespostaIaStreamListener listener) {
        ObjectNode corpo = montarCorpoRequisicao(systemPrompt, mensagens);
        corpo.put("stream", true);

        try {
            restClient.post()
                    .uri("/v1/messages")
                    .header("accept", "text/event-stream")
                    .body(corpo)
                    .exchange((request, response) -> {
                        try (InputStream corpoResposta = response.getBody();
                             Reader reader = new InputStreamReader(corpoResposta, StandardCharsets.UTF_8)) {
                            sseStreamProcessor.processar(reader, listener);
                        } catch (IOException e) {
                            listener.onErro(new AiConsultantException(
                                    "Falha ao ler o stream de resposta da Anthropic: " + e.getMessage(), e));
                        }
                        return null;
                    });
        } catch (RestClientException e) {
            listener.onErro(new AiConsultantException("Falha ao chamar a API da Anthropic (stream): " + e.getMessage(), e));
        }
    }

    private ObjectNode montarCorpoRequisicao(String systemPrompt, List<AiMensagem> mensagens) {
        ObjectNode corpo = JsonNodeFactory.instance.objectNode();
        corpo.put("model", properties.model());
        corpo.put("max_tokens", properties.maxTokens());
        corpo.put("system", systemPrompt);

        ArrayNode mensagensJson = corpo.putArray("messages");
        for (AiMensagem mensagem : mensagens) {
            ObjectNode mensagemJson = mensagensJson.addObject();
            mensagemJson.put("role", mensagem.role());

            if (mensagem.temImagem()) {
                ArrayNode blocos = mensagemJson.putArray("content");

                ObjectNode blocoImagem = blocos.addObject();
                blocoImagem.put("type", "image");
                ObjectNode fonte = blocoImagem.putObject("source");
                fonte.put("type", "base64");
                fonte.put("media_type", mensagem.imagemMediaType());
                fonte.put("data", mensagem.imagemBase64());

                ObjectNode blocoTexto = blocos.addObject();
                blocoTexto.put("type", "text");
                blocoTexto.put("text", mensagem.conteudo());
            } else {
                mensagemJson.put("content", mensagem.conteudo());
            }
        }

        // Ferramenta nativa da Anthropic (server-side, sem infra propria): permite
        // que o modelo pesquise na web quando a pergunta exigir informacao atual
        // ou local que ele nao tem (ex: fornecedores de uma cidade). Limitada a
        // poucas buscas por mensagem para controlar custo/latencia.
        ArrayNode ferramentas = corpo.putArray("tools");
        ObjectNode webSearch = ferramentas.addObject();
        webSearch.put("type", "web_search_20250305");
        webSearch.put("name", "web_search");
        webSearch.put("max_uses", properties.webSearchMaxUses());

        return corpo;
    }

    private String extrairTexto(JsonNode resposta) {
        if (resposta == null) {
            throw new AiConsultantException("Resposta vazia da API da Anthropic");
        }

        JsonNode conteudo = resposta.path("content");
        if (!conteudo.isArray() || conteudo.isEmpty()) {
            throw new AiConsultantException("Resposta da API da Anthropic sem conteudo: " + resposta);
        }

        StringBuilder texto = new StringBuilder();
        for (JsonNode bloco : conteudo) {
            if ("text".equals(bloco.path("type").asText())) {
                texto.append(bloco.path("text").asText());
            }
        }

        if (texto.isEmpty()) {
            throw new AiConsultantException("Resposta da API da Anthropic sem bloco de texto: " + resposta);
        }

        return texto.toString();
    }
}
