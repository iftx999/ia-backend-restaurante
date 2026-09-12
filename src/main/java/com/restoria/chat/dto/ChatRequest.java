package com.restoria.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param conversationId   id de uma conversa ja existente (na memoria do servidor).
 *                         Se nulo/ausente, uma nova conversa e iniciada.
 * @param mensagem          pergunta do usuario em linguagem natural (RF-01)
 * @param imagemBase64      imagem anexada (base64, sem o prefixo "data:..."), opcional.
 * @param imagemMediaType   media type da imagem anexada (ex: "image/png"), obrigatorio se
 *                          {@code imagemBase64} estiver presente.
 */
public record ChatRequest(
        String conversationId,
        @NotBlank(message = "mensagem nao pode ser vazia") String mensagem,
        String imagemBase64,
        String imagemMediaType
) {
    public ChatRequest(String conversationId, String mensagem) {
        this(conversationId, mensagem, null, null);
    }
}
