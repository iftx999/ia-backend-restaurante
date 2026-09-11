package com.restoria.analise;

import com.restoria.analise.dto.CompararRelatoriosResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioComparadorTest {

    @Test
    void calculaDeltaDeCmvEDeMargemPorPrato() {
        Relatorio anterior = relatorio(new BigDecimal("0.30"));
        anterior.adicionarIndicadorPrato(new IndicadorPrato("Feijoada", new BigDecimal("0.25"), false));
        anterior.adicionarIndicadorPrato(new IndicadorPrato("Moqueca", new BigDecimal("0.10"), true));

        Relatorio atual = relatorio(new BigDecimal("0.28"));
        atual.adicionarIndicadorPrato(new IndicadorPrato("Feijoada", new BigDecimal("0.32"), false));
        atual.adicionarIndicadorPrato(new IndicadorPrato("Bobó", new BigDecimal("0.40"), false));

        CompararRelatoriosResponse resposta = RelatorioComparador.comparar(atual, anterior);

        assertThat(resposta.deltaCmv()).isEqualByComparingTo("-0.02");
        assertThat(resposta.pratos()).hasSize(2);

        var feijoada = resposta.pratos().stream().filter(p -> p.nomePrato().equals("Feijoada")).findFirst().orElseThrow();
        assertThat(feijoada.margemAnterior()).isEqualByComparingTo("0.25");
        assertThat(feijoada.deltaMargem()).isEqualByComparingTo("0.07");

        var bobo = resposta.pratos().stream().filter(p -> p.nomePrato().equals("Bobó")).findFirst().orElseThrow();
        assertThat(bobo.margemAnterior()).isNull();
        assertThat(bobo.deltaMargem()).isNull();
    }

    @Test
    void naoQuebraQuandoCmvNaoCalculavelEmUmDosRelatorios() {
        Relatorio anterior = relatorio(null);
        Relatorio atual = relatorio(new BigDecimal("0.30"));

        CompararRelatoriosResponse resposta = RelatorioComparador.comparar(atual, anterior);

        assertThat(resposta.deltaCmv()).isNull();
    }

    private Relatorio relatorio(BigDecimal cmv) {
        return new Relatorio(null, null, null, cmv, "texto do relatorio");
    }
}
