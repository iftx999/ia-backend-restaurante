package com.restoria.chat.dto;

/** {@code role} usa os mesmos valores do modelo de UI do frontend ("user"/"assistant"). */
public record MensagemResponse(String role, String texto) {
}
