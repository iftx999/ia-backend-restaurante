package com.restoria.security.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificarEmailRequest(@NotBlank(message = "token nao pode ser vazio") String token) {
}
