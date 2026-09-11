package com.restoria.analise;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le uma planilha CSV/XLSX (RF-07/RF-08) e devolve cada linha como um mapa
 * "nome da coluna normalizado" -> valor em texto. Nao conhece o dominio
 * (vendas/estoque); os mapeadores especificos interpretam o conteudo.
 */
@Component
class PlanilhaLeitor {

    List<Map<String, String>> ler(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new PlanilhaInvalidaException("Arquivo enviado esta vazio");
        }

        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);

        try {
            if (nome.endsWith(".csv")) {
                return lerCsv(arquivo);
            } else if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
                return lerXlsx(arquivo);
            }
            throw new PlanilhaInvalidaException("Formato de arquivo nao suportado (esperado .csv ou .xlsx): " + nome);
        } catch (IOException | CsvException e) {
            throw new PlanilhaInvalidaException("Nao foi possivel ler o arquivo enviado: " + e.getMessage());
        }
    }

    private List<Map<String, String>> lerCsv(MultipartFile arquivo) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> linhas = reader.readAll();
            if (linhas.isEmpty()) {
                throw new PlanilhaInvalidaException("Planilha nao contem nenhuma linha");
            }
            String[] cabecalho = linhas.get(0);
            List<Map<String, String>> resultado = new ArrayList<>();
            for (int i = 1; i < linhas.size(); i++) {
                resultado.add(montarLinha(cabecalho, linhas.get(i)));
            }
            return resultado;
        }
    }

    private List<Map<String, String>> lerXlsx(MultipartFile arquivo) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row linhaCabecalho = sheet.getRow(sheet.getFirstRowNum());
            if (linhaCabecalho == null) {
                throw new PlanilhaInvalidaException("Planilha nao contem nenhuma linha");
            }

            String[] cabecalho = new String[linhaCabecalho.getLastCellNum()];
            for (int c = 0; c < cabecalho.length; c++) {
                Cell cell = linhaCabecalho.getCell(c);
                cabecalho[c] = cell == null ? "" : formatter.formatCellValue(cell);
            }

            List<Map<String, String>> resultado = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row linha = sheet.getRow(r);
                if (linha == null || ehLinhaVazia(linha, formatter)) {
                    continue;
                }
                String[] valores = new String[cabecalho.length];
                for (int c = 0; c < cabecalho.length; c++) {
                    Cell cell = linha.getCell(c);
                    valores[c] = cell == null ? "" : formatarValor(cell, formatter);
                }
                resultado.add(montarLinha(cabecalho, valores));
            }
            return resultado;
        }
    }

    private String formatarValor(Cell cell, DataFormatter formatter) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return formatter.formatCellValue(cell);
    }

    private boolean ehLinhaVazia(Row linha, DataFormatter formatter) {
        for (Cell cell : linha) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> montarLinha(String[] cabecalho, String[] valores) {
        Map<String, String> linha = new LinkedHashMap<>();
        for (int c = 0; c < cabecalho.length; c++) {
            String chave = normalizarNomeColuna(cabecalho[c]);
            String valor = c < valores.length && valores[c] != null ? valores[c].trim() : "";
            linha.put(chave, valor);
        }
        return linha;
    }

    private String normalizarNomeColuna(String nomeColuna) {
        return nomeColuna == null
                ? ""
                : nomeColuna.trim().toLowerCase(Locale.ROOT)
                    .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
                    .replace("é", "e").replace("ê", "e")
                    .replace("í", "i")
                    .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                    .replace("ú", "u")
                    .replace(" ", "_");
    }
}
