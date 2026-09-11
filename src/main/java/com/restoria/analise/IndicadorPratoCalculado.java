package com.restoria.analise;

import java.math.BigDecimal;

/**
 * Indicadores agregados de um prato dentro de um periodo de vendas (RF-09).
 * {@code margemPercentual} e {@code custoTotal} sao {@code null} quando a
 * planilha de vendas nao trouxe custo unitario para nenhuma linha do prato
 * (ver {@link ItemVenda#getCustoUnitario()}).
 */
record IndicadorPratoCalculado(
        String nomePrato,
        int quantidadeVendida,
        BigDecimal receita,
        BigDecimal custoTotal,
        BigDecimal margemPercentual,
        boolean alertaMargem) {
}
