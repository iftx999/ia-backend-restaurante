package com.restoria.analise;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanilhaLeitorTest {

    private final PlanilhaLeitor leitor = new PlanilhaLeitor();

    @Test
    void lancaExcecaoParaArquivoVazio() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "vendas.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> leitor.ler(arquivo))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void lancaExcecaoParaArquivoNulo() {
        assertThatThrownBy(() -> leitor.ler(null))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void lancaExcecaoParaFormatoNaoSuportado() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "vendas.pdf", "application/pdf",
                "conteudo qualquer".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> leitor.ler(arquivo))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("nao suportado");
    }

    @Test
    void csvComApenasCabecalhoNaoTemLinhasDeDados() {
        MockMultipartFile arquivo = csv("prato,quantidade,preco\n");

        List<Map<String, String>> linhas = leitor.ler(arquivo);

        assertThat(linhas).isEmpty();
    }

    @Test
    void csvPulaLinhasTotalmenteEmBrancoNoMeio() {
        MockMultipartFile arquivo = csv("""
                prato,quantidade,preco
                Feijoada,10,35.00
                ,,
                Moqueca,5,60.00
                """);

        List<Map<String, String>> linhas = leitor.ler(arquivo);

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).get("prato")).isEqualTo("Feijoada");
        assertThat(linhas.get(1).get("prato")).isEqualTo("Moqueca");
    }

    @Test
    void csvComCabecalhoDesconhecidoNaoMapeiaNenhumaColuna() {
        MockMultipartFile arquivo = csv("""
                coluna_a,coluna_b
                x,y
                """);

        List<Map<String, String>> linhas = leitor.ler(arquivo);

        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0)).doesNotContainKeys("prato", "quantidade", "preco");
    }

    private MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "vendas.csv", "text/csv",
                conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
