package com.restoria.integration.ai;

import java.util.List;

/**
 * Unico ponto de contato com a API de embeddings (Voyage AI) para a base de
 * conhecimento (RAG). Nenhum outro Service deve chamar essa API diretamente
 * (ver CLAUDE.md).
 *
 * Implementacao real: {@link VoyageEmbeddingClient}.
 */
public interface EmbeddingClient {

    /**
     * Gera um embedding para cada texto da lista, em lote (uma unica chamada).
     * A ordem dos vetores retornados corresponde a ordem de entrada.
     *
     * @param tipo DOCUMENTO ao indexar trechos da base, CONSULTA ao embedar a pergunta do usuario
     * @throws EmbeddingException se a chamada falhar (rede, timeout, resposta inesperada)
     */
    List<List<Float>> gerarEmbeddings(List<String> textos, TipoEmbedding tipo);
}
