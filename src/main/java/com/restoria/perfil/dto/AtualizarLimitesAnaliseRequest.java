package com.restoria.perfil.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * RF-10: limites usados para marcar anomalias no relatorio analitico,
 * expressos como fracao (0.20 = 20%). Configuraveis por usuario porque a
 * tolerancia varia por tipo de operacao (ex: rodizio vs. bistro a la carte).
 */
public record AtualizarLimitesAnaliseRequest(
        @NotNull(message = "margemMinimaEsperada nao pode ser vazia")
        @DecimalMin(value = "0.0", message = "margemMinimaEsperada nao pode ser negativa")
        @DecimalMax(value = "1.0", message = "margemMinimaEsperada nao pode ser maior que 1 (100%)")
        BigDecimal margemMinimaEsperada,

        @NotNull(message = "percentualPerdaAlerta nao pode ser vazio")
        @DecimalMin(value = "0.0", message = "percentualPerdaAlerta nao pode ser negativo")
        @DecimalMax(value = "1.0", message = "percentualPerdaAlerta nao pode ser maior que 1 (100%)")
        BigDecimal percentualPerdaAlerta
) {
}
