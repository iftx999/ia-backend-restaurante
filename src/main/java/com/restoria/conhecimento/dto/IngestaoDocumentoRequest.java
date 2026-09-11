package com.restoria.conhecimento.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param titulo   titulo do documento (RF-19)
 * @param conteudo texto completo do documento, sera quebrado em trechos (chunking)
 * @param fonte    origem/nome do arquivo, opcional
 */
public record IngestaoDocumentoRequest(
        @NotBlank(message = "titulo nao pode ser vazio") String titulo,
        @NotBlank(message = "conteudo nao pode ser vazio") String conteudo,
        String fonte
) {
}
