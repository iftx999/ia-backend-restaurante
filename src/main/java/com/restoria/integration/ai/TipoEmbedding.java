package com.restoria.integration.ai;

/**
 * A Voyage AI recomenda diferenciar o "input_type" usado para gerar o embedding
 * conforme o uso: documentos indexados (DOCUMENTO) vs. pergunta do usuario (CONSULTA).
 * Isso melhora a qualidade da busca por similaridade.
 */
public enum TipoEmbedding {
    DOCUMENTO("document"),
    CONSULTA("query");

    private final String valorApi;

    TipoEmbedding(String valorApi) {
        this.valorApi = valorApi;
    }

    public String valorApi() {
        return valorApi;
    }
}
