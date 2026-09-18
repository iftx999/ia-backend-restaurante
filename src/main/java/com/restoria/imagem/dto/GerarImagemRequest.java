package com.restoria.imagem.dto;

import jakarta.validation.constraints.NotBlank;

/** @param tamanho "quadrado" (padrao), "paisagem" ou "retrato" — ver {@link com.restoria.integration.ai.TamanhoImagem}. */
public record GerarImagemRequest(
        @NotBlank(message = "prompt nao pode ser vazio") String prompt,
        String tamanho
) {
}
