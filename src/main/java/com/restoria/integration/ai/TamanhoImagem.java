package com.restoria.integration.ai;

/** Tamanhos suportados pela API de imagens da OpenAI (familia GPT Image, ver {@link OpenAiImagemProperties}). */
public enum TamanhoImagem {
    QUADRADO("1024x1024"),
    PAISAGEM("1536x1024"),
    RETRATO("1024x1536");

    private final String valorApi;

    TamanhoImagem(String valorApi) {
        this.valorApi = valorApi;
    }

    public String valorApi() {
        return valorApi;
    }
}
