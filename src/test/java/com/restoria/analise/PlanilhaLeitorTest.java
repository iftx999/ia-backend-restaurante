package com.restoria.analise;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
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
    void xlsxLidoEmStreamingPulaLinhaVaziaEConverteNumeros() throws Exception {
        MockMultipartFile arquivo = xlsx(new Object[][] {
                {"Prato", "Quantidade", "Preco"},
                {"Feijoada", 10, 35.5},
                {null, null, null},
                {"Moqueca", 5, 60},
        });

        List<Map<String, String>> linhas = leitor.ler(arquivo);

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0)).containsEntry("prato", "Feijoada")
                .containsEntry("quantidade", "10.0")
                .containsEntry("preco", "35.5");
        assertThat(linhas.get(1)).containsEntry("prato", "Moqueca");
    }

    @Test
    void xlsxSemNenhumaLinhaLancaExcecao() throws Exception {
        MockMultipartFile arquivo = xlsx(new Object[0][]);

        assertThatThrownBy(() -> leitor.ler(arquivo))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("nenhuma linha");
    }

    @Test
    void csvAcimaDoLimiteDeLinhasELancaExcecao() {
        StringBuilder conteudo = new StringBuilder("prato,quantidade,preco\n");
        for (int i = 0; i <= PlanilhaLeitor.MAX_LINHAS; i++) {
            conteudo.append("Prato ").append(i).append(",1,10.00\n");
        }

        assertThatThrownBy(() -> leitor.ler(csv(conteudo.toString())))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("limite de " + PlanilhaLeitor.MAX_LINHAS);
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

    private MockMultipartFile xlsx(Object[][] linhas) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Vendas");
            for (int r = 0; r < linhas.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < linhas[r].length; c++) {
                    Object valor = linhas[r][c];
                    if (valor instanceof Number numero) {
                        row.createCell(c).setCellValue(numero.doubleValue());
                    } else if (valor != null) {
                        row.createCell(c).setCellValue(valor.toString());
                    }
                }
            }
            workbook.write(saida);
            return new MockMultipartFile("arquivo", "vendas.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", saida.toByteArray());
        }
    }
}
