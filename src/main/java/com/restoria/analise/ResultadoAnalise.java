package com.restoria.analise;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saida de {@link IndicadorCalculator}: indicadores numericos calculados em
 * Java puro a partir dos uploads de vendas/estoque, antes de qualquer
 * interpretacao em linguagem natural pela IA (ver regra de arquitetura em
 * CLAUDE.md - calculos nunca sao delegados a IA).
 */
record ResultadoAnalise(
        BigDecimal cmvCalculado,
        List<IndicadorPratoCalculado> indicadoresPrato,
        List<IndicadorInsumoCalculado> indicadoresInsumo,
        List<String> anomalias) {
}
