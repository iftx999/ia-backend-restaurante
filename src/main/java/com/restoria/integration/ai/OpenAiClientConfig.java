package com.restoria.integration.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, OpenAiImagemProperties.class})
public class OpenAiClientConfig {

    @Bean
    public RestClient openAiRestClient(OpenAiProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(timeout)
                        .withReadTimeout(timeout));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .defaultHeader("content-type", "application/json")
                .build();
    }

    /**
     * RestClient separado pra `/v1/images/*`: sem `content-type` fixo (o
     * endpoint de edicao usa multipart, nao JSON) e timeout maior (geracao de
     * imagem demora mais que uma mensagem de chat, ver
     * {@link OpenAiImagemProperties#timeoutSeconds()}).
     */
    @Bean
    public RestClient openAiImagemRestClient(OpenAiProperties properties, OpenAiImagemProperties imagemProperties) {
        Duration timeout = Duration.ofSeconds(imagemProperties.timeoutSeconds());
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(timeout)
                        .withReadTimeout(timeout));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
    }
}
