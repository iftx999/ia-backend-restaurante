package com.restoria.analise;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculo de indicadores (CMV, margem por prato, perda de insumos) e deteccao
 * de anomalias (RF-09/RF-10). Java puro, sem chamada a IA nem a banco — ver
 * regra de arquitetura em CLAUDE.md.
 *
 * <p>Limitacao conhecida (registrada em docs/03-modelo-dados.md): sem uma
 * ficha tecnica ligando prato -> insumos, nao ha como calcular o "giro de
 * estoque" classico (custo consumido / estoque medio), que exige saldo de
 * estoque no inicio/fim do periodo. Em vez disso, usamos o percentual de
 * perda por insumo (quantidade perdida / quantidade comprada) como indicador
 * de eficiencia de estoque — e o que a planilha atual permite calcular com
 * seguranca.
 */
@Component
class IndicadorCalculator {

    private static final BigDecimal MARGEM_MINIMA_ESPERADA_PADRAO = new BigDecimal("0.20");
    private static final BigDecimal PERCENTUAL_PERDA_ALERTA_PADRAO = new BigDecimal("0.05");
    private static final int ESCALA = 4;

    /** Usa os limites padrao (20% margem minima, 5% perda) — ver {@link com.restoria.shared.Usuario}. */
    ResultadoAnalise calcular(List<ItemVenda> vendas, List<ItemEstoque> estoque) {
        return calcular(vendas, estoque, MARGEM_MINIMA_ESPERADA_PADRAO, PERCENTUAL_PERDA_ALERTA_PADRAO);
    }

    /** RF-10: limites configuraveis por usuario (tolerancia varia por tipo de operacao). */
    ResultadoAnalise calcular(
            List<ItemVenda> vendas,
            List<ItemEstoque> estoque,
            BigDecimal margemMinimaEsperada,
            BigDecimal percentualPerdaAlerta) {
        List<IndicadorPratoCalculado> indicadoresPrato = calcularIndicadoresPrato(vendas, margemMinimaEsperada);
        List<IndicadorInsumoCalculado> indicadoresInsumo = calcularIndicadoresInsumo(estoque, percentualPerdaAlerta);
        BigDecimal cmv = calcularCmvGlobal(vendas, estoque);

        List<String> anomalias = new ArrayList<>();
        for (IndicadorPratoCalculado indicador : indicadoresPrato) {
            if (indicador.alertaMargem()) {
                anomalias.add("Prato \"%s\" com margem %s%.2f%%".formatted(
                        indicador.nomePrato(),
                        indicador.margemPercentual().signum() < 0 ? "negativa: " : "abaixo do esperado: ",
                        indicador.margemPercentual().multiply(BigDecimal.valueOf(100))));
            }
        }
        for (IndicadorInsumoCalculado indicador : indicadoresInsumo) {
            if (indicador.alertaPerda()) {
                anomalias.add("Insumo \"%s\" com alto indice de perda: %.2f%%".formatted(
                        indicador.nomeInsumo(),
                        indicador.percentualPerda().multiply(BigDecimal.valueOf(100))));
            }
        }

        return new ResultadoAnalise(cmv, indicadoresPrato, indicadoresInsumo, anomalias);
    }

    private List<IndicadorPratoCalculado> calcularIndicadoresPrato(List<ItemVenda> vendas, BigDecimal margemMinimaEsperada) {
        Map<String, List<ItemVenda>> porPrato = new LinkedHashMap<>();
        for (ItemVenda item : vendas) {
            porPrato.computeIfAbsent(item.getNomePrato(), k -> new ArrayList<>()).add(item);
        }

        List<IndicadorPratoCalculado> resultado = new ArrayList<>();
        for (Map.Entry<String, List<ItemVenda>> entry : porPrato.entrySet()) {
            int quantidadeTotal = 0;
            BigDecimal receita = BigDecimal.ZERO;
            BigDecimal custoTotal = BigDecimal.ZERO;
            boolean temCusto = true;

            for (ItemVenda item : entry.getValue()) {
                BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidadeVendida());
                quantidadeTotal += item.getQuantidadeVendida();
                receita = receita.add(item.getPrecoVenda().multiply(quantidade));

                if (item.getCustoUnitario() != null) {
                    custoTotal = custoTotal.add(item.getCustoUnitario().multiply(quantidade));
                } else {
                    temCusto = false;
                }
            }

            BigDecimal margem = null;
            boolean alerta = false;
            if (temCusto && receita.signum() > 0) {
                margem = receita.subtract(custoTotal).divide(receita, ESCALA, RoundingMode.HALF_UP);
                alerta = margem.compareTo(margemMinimaEsperada) < 0;
            }

            resultado.add(new IndicadorPratoCalculado(
                    entry.getKey(), quantidadeTotal, receita, temCusto ? custoTotal : null, margem, alerta));
        }
        return resultado;
    }

    private List<IndicadorInsumoCalculado> calcularIndicadoresInsumo(List<ItemEstoque> estoque, BigDecimal percentualPerdaAlerta) {
        Map<String, List<ItemEstoque>> porInsumo = new LinkedHashMap<>();
        for (ItemEstoque item : estoque) {
            porInsumo.computeIfAbsent(item.getNomeInsumo(), k -> new ArrayList<>()).add(item);
        }

        List<IndicadorInsumoCalculado> resultado = new ArrayList<>();
        for (Map.Entry<String, List<ItemEstoque>> entry : porInsumo.entrySet()) {
            BigDecimal quantidadeComprada = BigDecimal.ZERO;
            BigDecimal quantidadePerdida = BigDecimal.ZERO;
            boolean temPerda = true;

            for (ItemEstoque item : entry.getValue()) {
                quantidadeComprada = quantidadeComprada.add(item.getQuantidadeComprada());
                if (item.getQuantidadePerdida() != null) {
                    quantidadePerdida = quantidadePerdida.add(item.getQuantidadePerdida());
                } else {
                    temPerda = false;
                }
            }

            BigDecimal percentualPerda = null;
            boolean alerta = false;
            if (temPerda && quantidadeComprada.signum() > 0) {
                percentualPerda = quantidadePerdida.divide(quantidadeComprada, ESCALA, RoundingMode.HALF_UP);
                alerta = percentualPerda.compareTo(percentualPerdaAlerta) > 0;
            }

            resultado.add(new IndicadorInsumoCalculado(
                    entry.getKey(), quantidadeComprada, temPerda ? quantidadePerdida : null, percentualPerda, alerta));
        }
        return resultado;
    }

    /**
     * CMV simplificado (sem ficha tecnica): custo total de insumos comprados
     * no periodo / receita total de vendas no periodo. Pratica comum em
     * restaurantes pequenos quando nao ha custeio por prato detalhado.
     */
    private BigDecimal calcularCmvGlobal(List<ItemVenda> vendas, List<ItemEstoque> estoque) {
        BigDecimal receitaTotal = vendas.stream()
                .map(v -> v.getPrecoVenda().multiply(BigDecimal.valueOf(v.getQuantidadeVendida())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal custoTotalInsumos = estoque.stream()
                .map(e -> e.getCustoUnitario().multiply(e.getQuantidadeComprada()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (receitaTotal.signum() <= 0) {
            return null;
        }
        return custoTotalInsumos.divide(receitaTotal, ESCALA, RoundingMode.HALF_UP);
    }
}
