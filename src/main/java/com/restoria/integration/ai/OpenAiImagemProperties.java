package com.restoria.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.openai.imagens` do application.yml. Reaproveita
 * `restoria.openai.api-key`/`base-url` ({@link OpenAiProperties}) — mesma
 * chave da OpenAI, endpoint diferente (`/v1/images/*` em vez de `/v1/responses`).
 */
@ConfigurationProperties(prefix = "restoria.openai.imagens")
public record OpenAiImagemProperties(
        @DefaultValue("gpt-image-1") String modelo,
        @DefaultValue("60") int timeoutSeconds
) {
}
