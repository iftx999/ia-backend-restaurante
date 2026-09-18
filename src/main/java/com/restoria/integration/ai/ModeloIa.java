package com.restoria.integration.ai;

/**
 * Provedor de IA escolhido pelo usuario no chat consultivo (RF-22). Claude e
 * o padrao — GPT e uma troca manual, nao uma alternancia automatica por tipo
 * de pergunta (ver docs/05-multi-provedor-ia.md, secao 1).
 */
public enum ModeloIa {
    CLAUDE, GPT;

    /** Aceita "claude"/"gpt" (case-insensitive) vindo do frontend; null/vazio/desconhecido cai no padrao CLAUDE. */
    public static ModeloIa normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return CLAUDE;
        }
        return "gpt".equalsIgnoreCase(valor.trim()) ? GPT : CLAUDE;
    }
}
