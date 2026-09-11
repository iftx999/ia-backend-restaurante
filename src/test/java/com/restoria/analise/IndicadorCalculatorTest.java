package com.restoria.analise;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadorCalculatorTest {

    private final IndicadorCalculator calculator = new IndicadorCalculator();

    private ItemVenda venda(String prato, int quantidade, String precoVenda, String custoUnitario) {
        ItemVenda item = new ItemVenda();
        item.setNomePrato(prato);
        item.setQuantidadeVendida(quantidade);
        item.setPrecoVenda(new BigDecimal(precoVenda));
        item.setCustoUnitario(custoUnitario == null ? null : new BigDecimal(custoUnitario));
        return item;
    }

    private ItemEstoque estoque(String insumo, String quantidadeComprada, String custoUnitario, String quantidadePerdida) {
        ItemEstoque item = new ItemEstoque();
        item.setNomeInsumo(insumo);
        item.setQuantidadeComprada(new BigDecimal(quantidadeComprada));
        item.setCustoUnitario(new BigDecimal(custoUnitario));
        item.setQuantidadePerdida(quantidadePerdida == null ? null : new BigDecimal(quantidadePerdida));
        return item;
    }

    @Test
    void calculaMargemPositivaSemAlertaQuandoAcimaDoEsperado() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(venda("Feijoada", 10, "50.00", "20.00")), List.of());

        IndicadorPratoCalculado indicador = resultado.indicadoresPrato().get(0);
        assertThat(indicador.margemPercentual()).isEqualByComparingTo("0.6000");
        assertThat(indicador.alertaMargem()).isFalse();
    }

    @Test
    void marcaAlertaQuandoMargemAbaixoDoEsperado() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(venda("Prato caro", 5, "30.00", "27.00")), List.of());

        IndicadorPratoCalculado indicador = resultado.indicadoresPrato().get(0);
        assertThat(indicador.margemPercentual()).isEqualByComparingTo("0.1000");
        assertThat(indicador.alertaMargem()).isTrue();
        assertThat(resultado.anomalias()).anyMatch(a -> a.contains("Prato caro"));
    }

    @Test
    void marcaAlertaComMargemNegativa() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(venda("Prato no prejuizo", 3, "10.00", "15.00")), List.of());

        IndicadorPratoCalculado indicador = resultado.indicadoresPrato().get(0);
        assertThat(indicador.margemPercentual()).isEqualByComparingTo("-0.5000");
        assertThat(indicador.alertaMargem()).isTrue();
    }

    @Test
    void naoCalculaMargemQuandoFaltaCustoUnitario() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(venda("Prato sem custo", 4, "20.00", null)), List.of());

        IndicadorPratoCalculado indicador = resultado.indicadoresPrato().get(0);
        assertThat(indicador.margemPercentual()).isNull();
        assertThat(indicador.alertaMargem()).isFalse();
    }

    @Test
    void agrupaVariasLinhasDoMesmoPrato() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(
                        venda("Feijoada", 5, "50.00", "20.00"),
                        venda("Feijoada", 3, "50.00", "20.00")),
                List.of());

        assertThat(resultado.indicadoresPrato()).hasSize(1);
        IndicadorPratoCalculado indicador = resultado.indicadoresPrato().get(0);
        assertThat(indicador.quantidadeVendida()).isEqualTo(8);
        assertThat(indicador.receita()).isEqualByComparingTo("400.00");
    }

    @Test
    void marcaAlertaDePerdaQuandoAcimaDoLimite() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(), List.of(estoque("Tomate", "100", "2.00", "10")));

        IndicadorInsumoCalculado indicador = resultado.indicadoresInsumo().get(0);
        assertThat(indicador.percentualPerda()).isEqualByComparingTo("0.1000");
        assertThat(indicador.alertaPerda()).isTrue();
        assertThat(resultado.anomalias()).anyMatch(a -> a.contains("Tomate"));
    }

    @Test
    void naoMarcaAlertaDePerdaQuandoDentroDoLimite() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(), List.of(estoque("Arroz", "100", "5.00", "2")));

        IndicadorInsumoCalculado indicador = resultado.indicadoresInsumo().get(0);
        assertThat(indicador.percentualPerda()).isEqualByComparingTo("0.0200");
        assertThat(indicador.alertaPerda()).isFalse();
    }

    @Test
    void calculaCmvGlobalComoCustoDeInsumosSobreReceitaDeVendas() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(venda("Feijoada", 10, "50.00", "20.00")),
                List.of(estoque("Carne", "20", "10.00", null)));

        // receita = 10*50 = 500; custo insumos = 20*10 = 200; cmv = 200/500 = 0.40
        assertThat(resultado.cmvCalculado()).isEqualByComparingTo("0.4000");
    }

    @Test
    void cmvNuloQuandoNaoHaReceita() {
        ResultadoAnalise resultado = calculator.calcular(
                List.of(), List.of(estoque("Carne", "20", "10.00", null)));

        assertThat(resultado.cmvCalculado()).isNull();
    }

    @Test
    void respeitaLimiteDeMargemPersonalizadoDoUsuario() {
        // Margem de 60% passaria no limite padrao (20%), mas nao num limite mais rigoroso (70%).
        ResultadoAnalise resultado = calculator.calcular(
                List.of(venda("Feijoada", 10, "50.00", "20.00")), List.of(),
                new BigDecimal("0.70"), new BigDecimal("0.05"));

        IndicadorPratoCalculado indicador = resultado.indicadoresPrato().get(0);
        assertThat(indicador.margemPercentual()).isEqualByComparingTo("0.6000");
        assertThat(indicador.alertaMargem()).isTrue();
    }

    @Test
    void respeitaLimiteDePerdaPersonalizadoDoUsuario() {
        // 10% de perda dispararia alerta no limite padrao (5%), mas nao num limite mais tolerante (15%,
        // como um rodizio onde perda de insumo e naturalmente maior).
        ResultadoAnalise resultado = calculator.calcular(
                List.of(), List.of(estoque("Tomate", "100", "2.00", "10")),
                new BigDecimal("0.20"), new BigDecimal("0.15"));

        IndicadorInsumoCalculado indicador = resultado.indicadoresInsumo().get(0);
        assertThat(indicador.percentualPerda()).isEqualByComparingTo("0.1000");
        assertThat(indicador.alertaPerda()).isFalse();
    }
}
