package com.restoria.chat.dto;

import java.util.List;

/** Corpo da resposta de GET /api/chat/conversas/{id}. */
public record ConversaDetalheResponse(String id, String titulo, List<MensagemResponse> mensagens) {
}
