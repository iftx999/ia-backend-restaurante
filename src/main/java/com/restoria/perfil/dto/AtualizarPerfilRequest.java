package com.restoria.perfil.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarPerfilRequest(
        @NotBlank(message = "nome nao pode ser vazio") String nome,
        @NotBlank(message = "nomeRestaurante nao pode ser vazio") String nomeRestaurante
) {
}
