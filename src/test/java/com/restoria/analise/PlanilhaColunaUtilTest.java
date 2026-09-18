package com.restoria.analise;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Test
    void leTextoUsandoPrimeiroAliasPreenchido() {
        String valor = PlanilhaColunaUtil.texto(Map.of("nome_prato", "Feijoada"), "prato", "nome_prato", "item");
        assertThat(valor).isEqualTo("Feijoada");
    }

    @Test
    void textoRetornaNuloQuandoNenhumAliasPreenchido() {
        String valor = PlanilhaColunaUtil.texto(Map.of("prato", " "), "prato", "nome_prato");
        assertThat(valor).isNull();
    }

    @Test
    void leDataFormatoIso() {
        LocalDate data = PlanilhaColunaUtil.data(Map.of("data", "2026-09-18"), "data");
        assertThat(data).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void leDataFormatoBrasileiro() {
        LocalDate data = PlanilhaColunaUtil.data(Map.of("data", "18/09/2026"), "data");
        assertThat(data).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void dataRetornaNuloQuandoColunaAusente() {
        assertThat(PlanilhaColunaUtil.data(Map.of(), "data")).isNull();
    }

    @Test
    void lancaExcecaoParaDataInvalida() {
        assertThatThrownBy(() -> PlanilhaColunaUtil.data(Map.of("data", "31/31/2026"), "data"))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("Data invalida");
    }

    @Test
    void validarNaoNegativoNaoLancaParaValorPositivoOuNulo() {
        PlanilhaColunaUtil.validarNaoNegativo(new BigDecimal("10"), "quantidade");
        PlanilhaColunaUtil.validarNaoNegativo(null, "quantidade");
    }

    @Test
    void validarNaoNegativoLancaExcecaoParaValorNegativo() {
        assertThatThrownBy(() -> PlanilhaColunaUtil.validarNaoNegativo(new BigDecimal("-5"), "quantidade"))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("quantidade")
                .hasMessageContaining("negativo");
    }
}
