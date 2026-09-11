package com.restoria.integration.ai;

import java.util.List;

/**
 * Unico ponto de contato com IA para o modo consultivo. Nenhum outro Service
 * deve chamar a API externa diretamente (ver CLAUDE.md).
 *
 * Duas implementacoes: {@link AnthropicAiConsultantClient} (producao, chama a
 * API da Anthropic de verdade) e {@link MockAiConsultantClient} (dev local,
 * ativada via `restoria.ai.mock=true` / perfil Spring "mock").
 */
public interface AiConsultantClient {

    /**
     * Envia um system prompt + historico de mensagens e retorna o texto da resposta da IA.
     *
     * @throws AiConsultantException se a chamada falhar (rede, timeout, resposta inesperada)
     */
    String enviarMensagem(String systemPrompt, List<AiMensagem> mensagens);

    /**
     * Envia um system prompt + historico de mensagens e recebe a resposta em
     * streaming, token a token, via {@code listener}.
     *
     * Nao lanca excecao diretamente: falhas sao reportadas via {@link RespostaIaStreamListener#onErro(Throwable)}.
     */
    void enviarMensagemStream(String systemPrompt, List<AiMensagem> mensagens, RespostaIaStreamListener listener);
}
