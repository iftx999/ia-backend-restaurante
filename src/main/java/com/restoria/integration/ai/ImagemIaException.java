package com.restoria.integration.ai;

/**
 * Falha ao gerar/editar imagem via {@link ImagemIaClient} (rede, timeout,
 * resposta inesperada, ou recusa por moderacao de conteudo da OpenAI).
 */
public class ImagemIaException extends RuntimeException {

    private final boolean recusadaPorModeracao;

    public ImagemIaException(String mensagem) {
        this(mensagem, false);
    }

    public ImagemIaException(String mensagem, boolean recusadaPorModeracao) {
        super(mensagem);
        this.recusadaPorModeracao = recusadaPorModeracao;
    }

    public ImagemIaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.recusadaPorModeracao = false;
    }

    /** true quando a OpenAI recusou o prompt por violar a politica de conteudo (nao e falha de rede/timeout). */
    public boolean isRecusadaPorModeracao() {
        return recusadaPorModeracao;
    }
}
