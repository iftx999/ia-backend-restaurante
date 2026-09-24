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

    /**
     * Consultado a cada linha lida do stream. Quando {@code true} (ex: o
     * cliente fechou a aba), a leitura para e a conexao com o provedor e
     * fechada — sem isso a IA continuaria gerando (e cobrando) tokens que
     * ninguem vai ler.
     */
    default boolean cancelado() {
        return false;
    }

    /**
     * Chamado (em vez de {@link #onConcluido}/{@link #onErro}) quando a leitura
     * foi interrompida por {@link #cancelado()}, com o texto recebido ate ali.
     */
    default void onCancelado(String textoParcial) {
    }
}
