package com.restoria.analise.dto;

import jakarta.validation.constraints.NotBlank;

public record PerguntaRelatorioRequest(@NotBlank(message = "pergunta nao pode ser vazia") String pergunta) {
}
