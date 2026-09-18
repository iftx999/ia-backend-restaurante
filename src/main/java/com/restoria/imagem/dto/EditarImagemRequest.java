package com.restoria.imagem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param imagemBase64     imagem original (base64, sem o prefixo "data:...") — mesmo formato
 *                         ja usado no upload de imagem do chat consultivo.
 * @param imagemMediaType  media type da imagem original (ex: "image/png").
 * @param tamanho          "quadrado" (padrao), "paisagem" ou "retrato".
 */
public record EditarImagemRequest(
        @NotBlank(message = "prompt nao pode ser vazio") String prompt,
        @NotBlank(message = "imagemBase64 nao pode ser vazio") String imagemBase64,
        @NotBlank(message = "imagemMediaType nao pode ser vazio") String imagemMediaType,
        String tamanho
) {
}
