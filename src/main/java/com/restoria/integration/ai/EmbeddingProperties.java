package com.restoria.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.embeddings` do application.yml.
 * A api-key nunca tem default hardcoded: vem só de VOYAGE_API_KEY (ver CLAUDE.md).
 */
@ConfigurationProperties(prefix = "restoria.embeddings")
public record EmbeddingProperties(
        String apiKey,
        @DefaultValue("https://api.voyageai.com") String baseUrl,
        @DefaultValue("voyage-3.5") String model,
        @DefaultValue("1024") int outputDimension,
        @DefaultValue("30") int timeoutSeconds
) {
}
