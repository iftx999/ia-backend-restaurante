package com.restoria.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param conversationId id de uma conversa ja existente (na memoria do servidor).
 *                        Se nulo/ausente, uma nova conversa e iniciada.
 * @param mensagem        pergunta do usuario em linguagem natural (RF-01)
 */
public record ChatRequest(
        String conversationId,
        @NotBlank(message = "mensagem nao pode ser vazia") String mensagem
) {
}
