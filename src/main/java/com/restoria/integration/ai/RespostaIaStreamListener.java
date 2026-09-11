package com.restoria.integration.ai;

/**
 * Callback para receber a resposta da Anthropic em streaming (SSE), token a
 * token, ao inves de esperar a resposta inteira pronta.
 */
public interface RespostaIaStreamListener {

    /**
     * Chamado a cada pedaco de texto (`content_block_delta` / `text_delta`) recebido.
     */
    void onToken(String textoParcial);

    /**
     * Chamado quando o stream termina com sucesso, com o texto completo acumulado.
     */
    void onConcluido(String textoCompleto);

    /**
     * Chamado se a chamada falhar (rede, timeout, evento de erro da Anthropic).
     */
    void onErro(Throwable erro);
}
