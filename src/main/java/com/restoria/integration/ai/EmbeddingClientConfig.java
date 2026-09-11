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
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingClientConfig {

    @Bean
    public RestClient voyageRestClient(EmbeddingProperties properties) {
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
}
