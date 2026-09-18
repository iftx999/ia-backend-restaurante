package com.restoria.imagem.dto;

import com.restoria.imagem.ImagemPrato;

import java.time.LocalDateTime;

/** @param url caminho relativo de {@code GET /api/imagens/{id}/arquivo}, pronto pro frontend usar num {@code <img src>}. */
public record ImagemPratoResponse(Long id, String prompt, String tipoOperacao, String url, LocalDateTime criadaEm) {

    public static ImagemPratoResponse de(ImagemPrato imagem) {
        return new ImagemPratoResponse(
                imagem.getId(),
                imagem.getPrompt(),
                imagem.getTipoOperacao().name(),
                "/api/imagens/" + imagem.getId() + "/arquivo",
                imagem.getCriadaEm());
    }
}
