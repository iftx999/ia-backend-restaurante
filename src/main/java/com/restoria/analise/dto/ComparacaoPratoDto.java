package com.restoria.analise.dto;

import java.math.BigDecimal;

/**
 * Comparacao da margem de um prato entre dois relatorios (RF-15).
 * {@code margemAnterior}/{@code deltaMargem} vem null quando o prato nao
 * aparecia no relatorio anterior (item novo no cardapio, por exemplo).
 */
public record ComparacaoPratoDto(
        String nomePrato,
        BigDecimal margemAtual,
        BigDecimal margemAnterior,
        BigDecimal deltaMargem
) {
}
