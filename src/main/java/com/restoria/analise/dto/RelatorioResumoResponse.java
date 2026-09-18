package com.restoria.analise.dto;

import com.restoria.analise.Relatorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Item da lista de relatorios do usuario (RF-15: historico de relatorios). */
public record RelatorioResumoResponse(
        Long id, BigDecimal cmvCalculado, LocalDateTime geradoEm, boolean temAlerta, int quantidadeAlertas) {

    public static RelatorioResumoResponse de(Relatorio relatorio) {
        return new RelatorioResumoResponse(
                relatorio.getId(), relatorio.getCmvCalculado(), relatorio.getGeradoEm(),
                relatorio.isTemAlerta(), relatorio.getQuantidadeAlertas());
    }
}
