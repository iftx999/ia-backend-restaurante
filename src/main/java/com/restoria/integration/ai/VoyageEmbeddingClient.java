package com.restoria.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementacao real de {@link EmbeddingClient}: chama a API de embeddings
 * da Voyage AI (endpoint /v1/embeddings).
 */
@Component
public class VoyageEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final EmbeddingProperties properties;

    public VoyageEmbeddingClient(RestClient voyageRestClient, EmbeddingProperties properties) {
        this.restClient = voyageRestClient;
        this.properties = properties;
    }

    @Override
    public List<List<Float>> gerarEmbeddings(List<String> textos, TipoEmbedding tipo) {
        ObjectNode corpo = montarCorpoRequisicao(textos, tipo);

        try {
            JsonNode resposta = restClient.post()
                    .uri("/v1/embeddings")
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);

            return extrairEmbeddings(resposta);
        } catch (RestClientException e) {
            throw new EmbeddingException("Falha ao chamar a API de embeddings da Voyage AI: " + e.getMessage(), e);
        }
    }

    private ObjectNode montarCorpoRequisicao(List<String> textos, TipoEmbedding tipo) {
        ObjectNode corpo = JsonNodeFactory.instance.objectNode();
        ArrayNode inputJson = corpo.putArray("input");
        for (String texto : textos) {
            inputJson.add(texto);
        }
        corpo.put("model", properties.model());
        corpo.put("input_type", tipo.valorApi());
        corpo.put("output_dimension", properties.outputDimension());
        return corpo;
    }

    private List<List<Float>> extrairEmbeddings(JsonNode resposta) {
        if (resposta == null) {
            throw new EmbeddingException("Resposta vazia da API de embeddings da Voyage AI");
        }

        JsonNode data = resposta.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new EmbeddingException("Resposta da API de embeddings da Voyage AI sem dados: " + resposta);
        }

        List<JsonNode> itens = new ArrayList<>();
        data.forEach(itens::add);
        itens.sort(Comparator.comparingInt(item -> item.path("index").asInt()));

        List<List<Float>> embeddings = new ArrayList<>();
        for (JsonNode item : itens) {
            List<Float> vetor = new ArrayList<>();
            for (JsonNode valor : item.path("embedding")) {
                vetor.add((float) valor.asDouble());
            }
            embeddings.add(vetor);
        }
        return embeddings;
    }
}
