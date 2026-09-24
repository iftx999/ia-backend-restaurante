package com.restoria.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.openai.imagens` do application.yml. Reaproveita
 * `restoria.openai.api-key`/`base-url` ({@link OpenAiProperties}) — mesma
 * chave da OpenAI, endpoint diferente (`/v1/images/*` em vez de `/v1/responses`).
 *
 * <p>Modelos separados por operacao (familia GPT Image 2.5, lancada em
 * 2026-09-08): {@code flare} e otimizado pra velocidade/geracao do dia a dia,
 * {@code sunburst} troca tempo de geracao por precisao — mais adequado pra
 * edicao, onde preservar o que nao deveria mudar importa mais que latencia.
 *
 * @param maxConcorrencia maximo de chamadas simultaneas a API de imagens por instancia (bulkhead, ver {@link ControleChamadaIa}).
 */
@ConfigurationProperties(prefix = "restoria.openai.imagens")
public record OpenAiImagemProperties(
        @DefaultValue("gpt-image-2.5-flare") String modeloGeracao,
        @DefaultValue("gpt-image-2.5-sunburst") String modeloEdicao,
        @DefaultValue("60") int timeoutSeconds,
        @DefaultValue("16") int maxConcorrencia
) {
}
