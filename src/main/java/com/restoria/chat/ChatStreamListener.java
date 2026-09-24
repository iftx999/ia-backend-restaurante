package com.restoria.chat;

/**
 * Callback usado por {@link ChatService#responderStream} para notificar o
 * Controller conforme a resposta do modo consultivo vai chegando em
 * streaming. Contrato do Service para o Controller (nao envolve IA
 * diretamente, por isso fica em `chat`, nao em `integration.ai`).
 */
public interface ChatStreamListener {

    /**
     * Chamado assim que a conversa (nova ou existente) e resolvida, antes de
     * qualquer token de resposta.
     */
    void onConversaIniciada(String conversationId);

    /**
     * Chamado a cada pedaco de texto da resposta da IA.
     */
    void onToken(String textoParcial);

    /**
     * Chamado quando a resposta terminou e ja foi persistida no banco.
     */
    void onConcluido();

    /**
     * Chamado se algo falhar em qualquer etapa (chamada a IA, persistencia etc).
     */
    void onErro(Throwable erro);

    /** {@code true} quando o cliente desconectou e a geracao deve ser interrompida. */
    default boolean cancelado() {
        return false;
    }
}
