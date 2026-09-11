package com.restoria.analise;

import java.math.BigDecimal;

/**
 * Indicadores agregados de um insumo dentro de um periodo de estoque (RF-10).
 * {@code percentualPerda} e {@code null} quando nenhuma linha do insumo trouxe
 * quantidade perdida (coluna opcional em {@link ItemEstoque}).
 */
record IndicadorInsumoCalculado(
        String nomeInsumo,
        BigDecimal quantidadeComprada,
        BigDecimal quantidadePerdida,
        BigDecimal percentualPerda,
        boolean alertaPerda) {
}
