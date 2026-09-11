package com.restoria.analise;

import com.restoria.analise.dto.ComparacaoPratoDto;
import com.restoria.analise.dto.CompararRelatoriosResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compara dois relatorios do mesmo usuario (RF-15). Java puro, sem chamada a
 * IA nem a banco — mesma regra de arquitetura de {@link IndicadorCalculator}.
 *
 * <p>So compara pratos presentes no relatorio mais recente ({@code atual}):
 * um prato que sumiu do cardapio nao aparece na comparacao.
 */
final class RelatorioComparador {

    private RelatorioComparador() {
    }

    static CompararRelatoriosResponse comparar(Relatorio atual, Relatorio anterior) {
        Map<String, BigDecimal> margemAnteriorPorPrato = new LinkedHashMap<>();
        for (IndicadorPrato indicador : anterior.getIndicadoresPrato()) {
            margemAnteriorPorPrato.put(indicador.getNomePrato(), indicador.getMargemCalculada());
        }

        List<ComparacaoPratoDto> pratos = new ArrayList<>();
        for (IndicadorPrato indicador : atual.getIndicadoresPrato()) {
            BigDecimal margemAnterior = margemAnteriorPorPrato.get(indicador.getNomePrato());
            pratos.add(new ComparacaoPratoDto(
                    indicador.getNomePrato(),
                    indicador.getMargemCalculada(),
                    margemAnterior,
                    delta(indicador.getMargemCalculada(), margemAnterior)));
        }

        return new CompararRelatoriosResponse(
                atual.getId(), atual.getGeradoEm(), atual.getCmvCalculado(),
                anterior.getId(), anterior.getGeradoEm(), anterior.getCmvCalculado(),
                delta(atual.getCmvCalculado(), anterior.getCmvCalculado()),
                pratos);
    }

    private static BigDecimal delta(BigDecimal atual, BigDecimal anterior) {
        return atual == null || anterior == null ? null : atual.subtract(anterior);
    }
}
