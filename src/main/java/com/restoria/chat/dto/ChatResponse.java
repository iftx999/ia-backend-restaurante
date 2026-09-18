package com.restoria.chat.dto;

/** @param modeloIa "claude" ou "gpt" — qual provedor gerou a resposta (RF-22), pro frontend rotular o balao. */
public record ChatResponse(String conversationId, String resposta, String modeloIa) {
}
