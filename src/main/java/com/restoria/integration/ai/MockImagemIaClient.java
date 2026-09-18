package com.restoria.integration.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Implementacao falsa de {@link ImagemIaClient} para desenvolvimento local
 * sem gastar credito da API da OpenAI. Ativada com `restoria.ai.mock=true`
 * (mesma flag usada pro Claude/GPT em texto).
 *
 * Devolve sempre o mesmo PNG minusculo (1x1 pixel cinza) — nao chama nenhuma
 * API externa, so permite testar o fluxo de ponta a ponta (upload/prompt ->
 * backend -> storage -> resposta) sem depender de rede/chave.
 */
@Component
@ConditionalOnProperty(prefix = "restoria.ai", name = "mock", havingValue = "true")
public class MockImagemIaClient implements ImagemIaClient {

    // PNG 1x1 cinza (#808080), gerado uma vez e codificado como constante.
    private static final byte[] PNG_PLACEHOLDER = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Override
    public ImagemGerada gerar(String prompt, TamanhoImagem tamanho) {
        return new ImagemGerada(PNG_PLACEHOLDER, "image/png");
    }

    @Override
    public ImagemGerada editar(String prompt, byte[] imagemOriginal, String mediaTypeOriginal, TamanhoImagem tamanho) {
        return new ImagemGerada(PNG_PLACEHOLDER, "image/png");
    }
}
