package com.restoria.analise;

import java.math.BigDecimal;

/**
 * Formata os indicadores ja calculados (ver {@link IndicadorCalculator}) num
 * texto que a IA recebe como entrada para gerar o relatorio (RF-11). A IA so
 * enxerga esse texto — nunca os dados brutos da planilha.
 */
final class AnaliseResumoFormatter {

    private AnaliseResumoFormatter() {
    }

    static String formatar(ResultadoAnalise resultado) {
        StringBuilder sb = new StringBuilder();
        sb.append("Indicadores calculados a partir das planilhas enviadas pelo restaurante:\n\n");

        sb.append("CMV global (custo de insumos comprados / receita de vendas no periodo): ");
        sb.append(resultado.cmvCalculado() == null
                ? "nao calculavel (faltam dados de vendas ou de estoque)\n\n"
                : percentual(resultado.cmvCalculado()) + "\n\n");

        sb.append("Indicadores por prato:\n");
        if (resultado.indicadoresPrato().isEmpty()) {
            sb.append("(nenhum dado de vendas enviado)\n");
        } else {
            for (IndicadorPratoCalculado i : resultado.indicadoresPrato()) {
                sb.append("- %s: %d unidades vendidas, receita R$ %s, margem %s%s\n".formatted(
                        i.nomePrato(), i.quantidadeVendida(), i.receita(),
                        i.margemPercentual() == null ? "nao calculavel (sem custo unitario na planilha)" : percentual(i.margemPercentual()),
                        i.alertaMargem() ? " [ALERTA: abaixo do esperado]" : ""));
            }
        }

        sb.append("\nIndicadores por insumo:\n");
        if (resultado.indicadoresInsumo().isEmpty()) {
            sb.append("(nenhum dado de estoque enviado)\n");
        } else {
            for (IndicadorInsumoCalculado i : resultado.indicadoresInsumo()) {
                sb.append("- %s: %s comprado, perda %s%s\n".formatted(
                        i.nomeInsumo(), i.quantidadeComprada(),
                        i.percentualPerda() == null ? "nao informada" : percentual(i.percentualPerda()),
                        i.alertaPerda() ? " [ALERTA: indice de perda alto]" : ""));
            }
        }

        if (!resultado.anomalias().isEmpty()) {
            sb.append("\nAnomalias detectadas:\n");
            resultado.anomalias().forEach(a -> sb.append("- ").append(a).append('\n'));
        }

        return sb.toString();
    }

    private static String percentual(BigDecimal valor) {
        return valor.multiply(BigDecimal.valueOf(100)) + "%";
    }
}
