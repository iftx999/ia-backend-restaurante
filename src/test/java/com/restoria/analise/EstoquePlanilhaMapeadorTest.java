package com.restoria.analise;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstoquePlanilhaMapeadorTest {

    private final EstoquePlanilhaMapeador mapeador = new EstoquePlanilhaMapeador(new PlanilhaLeitor());
    private final UploadPlanilha upload = new UploadPlanilha(null, TipoPlanilha.ESTOQUE, "estoque.csv");

    @Test
    void mapeiaLinhasValidas() {
        MockMultipartFile arquivo = csv("""
                insumo,quantidade,custo,perda
                Farinha,100,2.50,5
                Carne,50,18.00,1
                """);

        List<ItemEstoque> itens = mapeador.mapear(arquivo, upload);

        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).getNomeInsumo()).isEqualTo("Farinha");
    }

    @Test
    void acumulaUmErroPorLinhaInvalidaEmVezDePararNaPrimeira() {
        MockMultipartFile arquivo = csv("""
                insumo,quantidade,custo,perda
                ,100,2.50,5
                Carne,50,abc,1
                Alho,20,3.00,2
                """);

        assertThatThrownBy(() -> mapeador.mapear(arquivo, upload))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .satisfies(e -> {
                    List<String> erros = ((PlanilhaInvalidaException) e).getErros();
                    assertThat(erros).hasSize(2);
                    assertThat(erros.get(0)).contains("Linha 2").contains("insumo");
                    assertThat(erros.get(1)).contains("Linha 3").contains("Valor numerico invalido");
                });
    }

    private MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "estoque.csv", "text/csv",
                conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
