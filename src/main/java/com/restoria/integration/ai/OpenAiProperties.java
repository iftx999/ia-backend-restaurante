package com.restoria.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.openai` do application.yml, espelhando
 * {@link AiProperties}. A api-key nunca tem default hardcoded: vem so de
 * OPENAI_API_KEY (ver CLAUDE.md).
 *
 * <p>Sem campo {@code mock} proprio: {@link MockOpenAiConsultantClient} e
 * ativado pela mesma flag {@code restoria.ai.mock} usada pro Claude (ver
 * docs/05-multi-provedor-ia.md, secao 3.8) — um so modo mock pro dev local,
 * nao dois flags independentes.
 *
 * @param webSearchMaxUses limite de buscas na web por mensagem enviada, mesma
 *                         ideia de {@link AiProperties#webSearchMaxUses()}.
 */
@ConfigurationProperties(prefix = "restoria.openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        int maxTokens,
        int timeoutSeconds,
        @DefaultValue("3") int webSearchMaxUses
) {
}
