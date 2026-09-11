package com.restoria.analise;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendasPlanilhaMapeadorTest {

    private final VendasPlanilhaMapeador mapeador = new VendasPlanilhaMapeador(new PlanilhaLeitor());
    private final UploadPlanilha upload = new UploadPlanilha(null, TipoPlanilha.VENDAS, "vendas.csv");

    @Test
    void mapeiaLinhasValidas() {
        MockMultipartFile arquivo = csv("""
                prato,quantidade,preco,custo
                Feijoada,10,35.00,15.00
                Moqueca,5,60.00,20.00
                """);

        List<ItemVenda> itens = mapeador.mapear(arquivo, upload);

        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).getNomePrato()).isEqualTo("Feijoada");
    }

    @Test
    void acumulaUmErroPorLinhaInvalidaEmVezDePararNaPrimeira() {
        MockMultipartFile arquivo = csv("""
                prato,quantidade,preco,custo
                ,10,35.00,15.00
                Moqueca,abc,60.00,20.00
                Bobó,5,,20.00
                Vatapá,3,40.00,10.00
                """);

        assertThatThrownBy(() -> mapeador.mapear(arquivo, upload))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .satisfies(e -> {
                    List<String> erros = ((PlanilhaInvalidaException) e).getErros();
                    assertThat(erros).hasSize(3);
                    assertThat(erros.get(0)).contains("Linha 2").contains("prato");
                    assertThat(erros.get(1)).contains("Linha 3");
                    assertThat(erros.get(2)).contains("Linha 4").contains("preco");
                });
    }

    private MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "vendas.csv", "text/csv",
                conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
