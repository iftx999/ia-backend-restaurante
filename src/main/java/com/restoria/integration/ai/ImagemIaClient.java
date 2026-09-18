package com.restoria.integration.ai;

/**
 * Unico ponto de contato com a API de imagens da OpenAI (`/v1/images/*`).
 * Nenhum outro Service deve chamar a API externa diretamente (ver CLAUDE.md).
 *
 * Duas implementacoes: {@link OpenAiImagemClient} (producao) e
 * {@link MockImagemIaClient} (dev local, `restoria.ai.mock=true`).
 */
public interface ImagemIaClient {

    /**
     * Gera uma imagem nova a partir de um prompt de texto.
     *
     * @throws ImagemIaException se a chamada falhar ou a OpenAI recusar o prompt por moderacao
     */
    ImagemGerada gerar(String prompt, TamanhoImagem tamanho);

    /**
     * Edita uma imagem existente a partir de um prompt (ex: trocar fundo/iluminacao).
     *
     * @throws ImagemIaException se a chamada falhar ou a OpenAI recusar o prompt por moderacao
     */
    ImagemGerada editar(String prompt, byte[] imagemOriginal, String mediaTypeOriginal, TamanhoImagem tamanho);
}
