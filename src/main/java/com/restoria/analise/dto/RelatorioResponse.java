package com.restoria.analise.dto;

import com.restoria.analise.Relatorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RelatorioResponse(
        Long id,
        BigDecimal cmvCalculado,
        String conteudoTextoIA,
        LocalDateTime geradoEm,
        List<IndicadorPratoDto> indicadoresPrato
) {

    public static RelatorioResponse de(Relatorio relatorio) {
        return new RelatorioResponse(
                relatorio.getId(),
                relatorio.getCmvCalculado(),
                relatorio.getConteudoTextoIA(),
                relatorio.getGeradoEm(),
                relatorio.getIndicadoresPrato().stream().map(IndicadorPratoDto::de).toList());
    }
}
