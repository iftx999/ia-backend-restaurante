package com.restoria.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "nome nao pode ser vazio") String nome,
        @NotBlank(message = "email nao pode ser vazio") @Email(message = "email invalido") String email,
        @NotBlank(message = "senha nao pode ser vazia") @Size(min = 6, message = "senha deve ter pelo menos 6 caracteres") String senha,
        @NotBlank(message = "nomeRestaurante nao pode ser vazio") String nomeRestaurante
) {
}
