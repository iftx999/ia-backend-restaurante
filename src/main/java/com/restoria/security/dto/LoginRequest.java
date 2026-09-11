package com.restoria.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email nao pode ser vazio") @Email(message = "email invalido") String email,
        @NotBlank(message = "senha nao pode ser vazia") String senha
) {
}
