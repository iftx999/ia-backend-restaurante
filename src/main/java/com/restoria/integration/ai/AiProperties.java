package com.restoria.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.ai` do application.yml.
 * A api-key nunca tem default hardcoded: vem só de ANTHROPIC_API_KEY (ver CLAUDE.md).
 *
 * @param mock quando true, usa {@link MockAiConsultantClient} em vez de chamar
 *             a API da Anthropic de verdade (uso local, sem gastar credito). Ver
 *             perfil Spring "mock" (application-mock.yml).
 */
@ConfigurationProperties(prefix = "restoria.ai")
public record AiProperties(
        String apiKey,
        String baseUrl,
        String model,
        int maxTokens,
        int timeoutSeconds,
        @DefaultValue("false") boolean mock
) {
}
