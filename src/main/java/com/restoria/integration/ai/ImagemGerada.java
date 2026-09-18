package com.restoria.integration.ai;

/** Imagem devolvida por {@link ImagemIaClient} (bytes decodificados do base64 da OpenAI). */
public record ImagemGerada(byte[] dados, String mediaType) {
}
