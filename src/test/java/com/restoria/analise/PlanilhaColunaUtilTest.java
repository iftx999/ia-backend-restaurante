package com.restoria.analise;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanilhaColunaUtilTest {

    @Test
    void leNumeroComPontoDecimal() {
        BigDecimal valor = PlanilhaColunaUtil.numero(Map.of("preco", "45.00"), "preco");
        assertThat(valor).isEqualByComparingTo("45.00");
    }

    @Test
    void leNumeroComVirgulaDecimalEPontoDeMilhar() {
        BigDecimal valor = PlanilhaColunaUtil.numero(Map.of("preco", "1.234,56"), "preco");
        assertThat(valor).isEqualByComparingTo("1234.56");
    }

    @Test
    void leNumeroComVirgulaDecimalSemMilhar() {
        BigDecimal valor = PlanilhaColunaUtil.numero(Map.of("preco", "45,50"), "preco");
        assertThat(valor).isEqualByComparingTo("45.50");
    }

    @Test
    void leNumeroInteiro() {
        BigDecimal valor = PlanilhaColunaUtil.numero(Map.of("quantidade", "30"), "quantidade");
        assertThat(valor).isEqualByComparingTo("30");
    }

    @Test
    void lancaExcecaoParaValorNaoNumerico() {
        assertThatThrownBy(() -> PlanilhaColunaUtil.numero(Map.of("preco", "abc"), "preco"))
                .isInstanceOf(PlanilhaInvalidaException.class);
    }
}
