package com.restoria.analise.dto;

import com.restoria.analise.IndicadorPrato;

import java.math.BigDecimal;

public record IndicadorPratoDto(String nomePrato, BigDecimal margemCalculada, boolean alerta) {

    public static IndicadorPratoDto de(IndicadorPrato indicador) {
        return new IndicadorPratoDto(indicador.getNomePrato(), indicador.getMargemCalculada(), indicador.isAlerta());
    }
}
