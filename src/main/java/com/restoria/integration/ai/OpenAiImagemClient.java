package com.restoria.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;

/**
 * Implementacao real de {@link ImagemIaClient}: chama a API de imagens da
 * OpenAI (`/v1/images/generations` e `/v1/images/edits`). Modelo diferente
 * por operacao — ver {@link OpenAiImagemProperties}. Ativa por padrao;
 * desativada quando `restoria.ai.mock=true` (ver {@link MockImagemIaClient}).
 */
@Component
@ConditionalOnProperty(prefix = "restoria.ai", name = "mock", havingValue = "false", matchIfMissing = true)
public class OpenAiImagemClient implements ImagemIaClient {

    private final RestClient restClient;
    private final OpenAiImagemProperties properties;

    public OpenAiImagemClient(RestClient openAiImagemRestClient, OpenAiImagemProperties properties) {
        this.restClient = openAiImagemRestClient;
        this.properties = properties;
    }

    @Override
    public ImagemGerada gerar(String prompt, TamanhoImagem tamanho) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("model", properties.modeloGeracao());
        corpo.add("prompt", prompt);
        corpo.add("size", tamanho.valorApi());
        corpo.add("n", 1);

        return chamar("/v1/images/generations", corpo);
    }

    @Override
    public ImagemGerada editar(String prompt, byte[] imagemOriginal, String mediaTypeOriginal, TamanhoImagem tamanho) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("model", properties.modeloEdicao());
        corpo.add("prompt", prompt);
        corpo.add("size", tamanho.valorApi());
        corpo.add("image", new ByteArrayResource(imagemOriginal) {
            @Override
            public String getFilename() {
                return "imagem-original." + extensaoDe(mediaTypeOriginal);
            }
        });

        return chamar("/v1/images/edits", corpo);
    }

    private ImagemGerada chamar(String uri, MultiValueMap<String, Object> corpo) {
        try {
            JsonNode resposta = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);

            return extrairImagem(resposta);
        } catch (RestClientResponseException e) {
            throw mapearErro(e);
        } catch (RestClientException e) {
            throw new ImagemIaException("Falha ao chamar a API de imagens da OpenAI: " + e.getMessage());
        }
    }

    private ImagemGerada extrairImagem(JsonNode resposta) {
        if (resposta == null) {
            throw new ImagemIaException("Resposta vazia da API de imagens da OpenAI");
        }

        JsonNode dados = resposta.path("data");
        if (!dados.isArray() || dados.isEmpty()) {
            throw new ImagemIaException("Resposta da API de imagens da OpenAI sem imagem: " + resposta);
        }

        String b64 = dados.get(0).path("b64_json").asText(null);
        if (b64 == null || b64.isBlank()) {
            throw new ImagemIaException("Resposta da API de imagens da OpenAI sem dados base64: " + resposta);
        }

        return new ImagemGerada(Base64.getDecoder().decode(b64), "image/png");
    }

    /**
     * Prompts recusados por moderacao de conteudo vem como 400 com
     * `error.code = "content_policy_violation"` — tratados separado de
     * falha de rede/timeout pra dar uma mensagem clara ao usuario em vez de
     * um erro generico (ver docs/06-geracao-imagem-ia.md, secao 3.6).
     */
    private ImagemIaException mapearErro(RestClientResponseException e) {
        String corpo = e.getResponseBodyAs(String.class);
        boolean moderacao = corpo != null && corpo.contains("content_policy_violation");
        String mensagem = moderacao
                ? "Nao foi possivel gerar essa imagem: o pedido viola a politica de conteudo da OpenAI. Tente reformular o prompt."
                : "Falha ao chamar a API de imagens da OpenAI (HTTP " + e.getStatusCode().value() + "): " + e.getMessage();
        return new ImagemIaException(mensagem, moderacao);
    }

    private String extensaoDe(String mediaType) {
        if (mediaType == null) {
            return "png";
        }
        return switch (mediaType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "png";
        };
    }
}
