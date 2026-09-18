package com.restoria.integration.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModeloIaTest {

    @Test
    void normalizaGptIgnorandoCaixa() {
        assertThat(ModeloIa.normalizar("gpt")).isEqualTo(ModeloIa.GPT);
        assertThat(ModeloIa.normalizar("GPT")).isEqualTo(ModeloIa.GPT);
        assertThat(ModeloIa.normalizar("Gpt")).isEqualTo(ModeloIa.GPT);
    }

    @Test
    void caiParaClaudeQuandoNuloVazioOuDesconhecido() {
        assertThat(ModeloIa.normalizar(null)).isEqualTo(ModeloIa.CLAUDE);
        assertThat(ModeloIa.normalizar("")).isEqualTo(ModeloIa.CLAUDE);
        assertThat(ModeloIa.normalizar("   ")).isEqualTo(ModeloIa.CLAUDE);
        assertThat(ModeloIa.normalizar("claude")).isEqualTo(ModeloIa.CLAUDE);
        assertThat(ModeloIa.normalizar("gemini")).isEqualTo(ModeloIa.CLAUDE);
    }
}
