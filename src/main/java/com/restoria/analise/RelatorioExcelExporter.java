package com.restoria.analise;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Exporta um {@link Relatorio} em planilha XLSX (indicadores por prato,
 * pensado para o gerente conferir/filtrar numa ferramenta que ele ja
 * conhece).
 */
@Component
class RelatorioExcelExporter {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    byte[] exportar(Relatorio relatorio) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Relatorio");

            CellStyle estiloTitulo = criarEstiloNegrito(workbook, 13);
            CellStyle estiloCabecalho = criarEstiloNegrito(workbook, 11);

            int linhaAtual = 0;

            Row tituloRow = sheet.createRow(linhaAtual++);
            criarCelula(tituloRow, 0, "Relatorio RestorIA - " + relatorio.getGeradoEm().format(FORMATO_DATA), estiloTitulo);

            linhaAtual++;
            Row cmvRow = sheet.createRow(linhaAtual++);
            criarCelula(cmvRow, 0, "CMV calculado", estiloCabecalho);
            criarCelula(cmvRow, 1, relatorio.getCmvCalculado() == null ? "nao calculavel" : formatarPercentual(relatorio.getCmvCalculado()), null);

            linhaAtual++;
            Row cabecalhoIndicadores = sheet.createRow(linhaAtual++);
            criarCelula(cabecalhoIndicadores, 0, "Prato", estiloCabecalho);
            criarCelula(cabecalhoIndicadores, 1, "Margem", estiloCabecalho);
            criarCelula(cabecalhoIndicadores, 2, "Alerta", estiloCabecalho);

            for (IndicadorPrato indicador : relatorio.getIndicadoresPrato()) {
                Row row = sheet.createRow(linhaAtual++);
                criarCelula(row, 0, indicador.getNomePrato(), null);
                criarCelula(row, 1, formatarPercentual(indicador.getMargemCalculada()), null);
                criarCelula(row, 2, indicador.isAlerta() ? "SIM" : "", null);
            }

            linhaAtual++;
            Row cabecalhoTexto = sheet.createRow(linhaAtual++);
            criarCelula(cabecalhoTexto, 0, "Analise da IA", estiloCabecalho);
            for (String paragrafo : relatorio.getConteudoTextoIA().split("\n")) {
                Row row = sheet.createRow(linhaAtual++);
                criarCelula(row, 0, paragrafo, null);
            }

            for (int c = 0; c < 3; c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar planilha do relatorio", e);
        }
    }

    private CellStyle criarEstiloNegrito(XSSFWorkbook workbook, int tamanhoFonte) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) tamanhoFonte);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void criarCelula(Row row, int coluna, String valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor);
        if (estilo != null) {
            cell.setCellStyle(estilo);
        }
    }

    private String formatarPercentual(BigDecimal valor) {
        return valor.multiply(BigDecimal.valueOf(100)) + "%";
    }
}
