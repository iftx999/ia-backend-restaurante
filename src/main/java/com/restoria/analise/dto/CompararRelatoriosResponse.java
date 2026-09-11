package com.restoria.analise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comparacao entre dois relatorios do mesmo usuario (RF-15: comparacao
 * mes a mes). {@code deltaCmv} e {@code deltaMargem} de cada prato vem null
 * quando um dos dois relatorios nao tem CMV/margem calculavel (ver
 * limitacoes documentadas em {@code IndicadorCalculator}).
 */
public record CompararRelatoriosResponse(
        Long idAtual,
        LocalDateTime geradoEmAtual,
        BigDecimal cmvAtual,
        Long idAnterior,
        LocalDateTime geradoEmAnterior,
        BigDecimal cmvAnterior,
        BigDecimal deltaCmv,
        List<ComparacaoPratoDto> pratos
) {
}
