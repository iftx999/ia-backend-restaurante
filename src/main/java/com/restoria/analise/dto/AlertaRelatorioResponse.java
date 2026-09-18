package com.restoria.analise.dto;

import com.restoria.analise.Relatorio;

import java.time.LocalDateTime;

/**
 * RF-16: estado de alerta do relatorio mais recente do usuario, usado pro
 * banner proativo no chat (sem o usuario precisar abrir o modulo analitico).
 */
public record AlertaRelatorioResponse(
        boolean temAlerta, Long relatorioId, int quantidadeAlertas, LocalDateTime geradoEm) {

    private static final AlertaRelatorioResponse SEM_RELATORIO = new AlertaRelatorioResponse(false, null, 0, null);

    public static AlertaRelatorioResponse semRelatorio() {
        return SEM_RELATORIO;
    }

    public static AlertaRelatorioResponse de(Relatorio relatorio) {
        return new AlertaRelatorioResponse(
                relatorio.isTemAlerta(), relatorio.getId(), relatorio.getQuantidadeAlertas(), relatorio.getGeradoEm());
    }
}
