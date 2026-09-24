package com.restoria.analise;

import com.opencsv.CSVReader;
import com.github.pjfanning.xlsx.StreamingReader;
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
import java.io.InputStream;
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

    /** Teto de linhas por arquivo: protege memoria e o tempo do insert. */
    static final int MAX_LINHAS = 100_000;

    private static final int LINHAS_EM_CACHE = 100;

    List<Map<String, String>> ler(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new PlanilhaInvalidaException("Arquivo enviado esta vazio");
        }

        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);

        try {
            if (nome.endsWith(".csv")) {
                return lerCsv(arquivo);
            } else if (nome.endsWith(".xlsx")) {
                return lerXlsx(arquivo, false);
            } else if (nome.endsWith(".xls")) {
                return lerXlsx(arquivo, true);
            }
            throw new PlanilhaInvalidaException("Formato de arquivo nao suportado (esperado .csv ou .xlsx): " + nome);
        } catch (IOException | CsvException e) {
            throw new PlanilhaInvalidaException("Nao foi possivel ler o arquivo enviado: " + e.getMessage());
        }
    }

    private List<Map<String, String>> lerCsv(MultipartFile arquivo) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String[] cabecalho = reader.readNext();
            if (cabecalho == null) {
                throw new PlanilhaInvalidaException("Planilha nao contem nenhuma linha");
            }
            List<Map<String, String>> resultado = new ArrayList<>();
            String[] valores;
            while ((valores = reader.readNext()) != null) {
                if (ehLinhaVazia(valores)) {
                    continue;
                }
                adicionarLinha(resultado, montarLinha(cabecalho, valores));
            }
            return resultado;
        }
    }

    /**
     * .xlsx e lido em streaming (excel-streaming-reader, SAX): so
     * {@link #LINHAS_EM_CACHE} linhas ficam em memoria por vez, em vez da
     * planilha inteira como no XSSFWorkbook — um .xlsx de 10MB podia ocupar
     * centenas de MB de heap. O .xls antigo (binario, max. 65 mil linhas)
     * continua pelo WorkbookFactory.
     */
    private List<Map<String, String>> lerXlsx(MultipartFile arquivo, boolean xlsLegado) throws IOException {
        try (InputStream entrada = arquivo.getInputStream();
             Workbook workbook = xlsLegado
                     ? WorkbookFactory.create(entrada)
                     : StreamingReader.builder().rowCacheSize(LINHAS_EM_CACHE).bufferSize(4096).open(entrada)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            String[] cabecalho = null;
            List<Map<String, String>> resultado = new ArrayList<>();
            for (Row linha : sheet) {
                if (cabecalho == null) {
                    cabecalho = new String[Math.max(0, linha.getLastCellNum())];
                    for (int c = 0; c < cabecalho.length; c++) {
                        Cell cell = linha.getCell(c);
                        cabecalho[c] = cell == null ? "" : formatter.formatCellValue(cell);
                    }
                    continue;
                }
                if (ehLinhaVazia(linha, formatter)) {
                    continue;
                }
                String[] valores = new String[cabecalho.length];
                for (int c = 0; c < cabecalho.length; c++) {
                    Cell cell = linha.getCell(c);
                    valores[c] = cell == null ? "" : formatarValor(cell, formatter);
                }
                adicionarLinha(resultado, montarLinha(cabecalho, valores));
            }

            if (cabecalho == null) {
                throw new PlanilhaInvalidaException("Planilha nao contem nenhuma linha");
            }
            return resultado;
        }
    }

    private void adicionarLinha(List<Map<String, String>> resultado, Map<String, String> linha) {
        if (resultado.size() >= MAX_LINHAS) {
            throw new PlanilhaInvalidaException(
                    "Planilha excede o limite de " + MAX_LINHAS + " linhas. Divida o arquivo por periodo.");
        }
        resultado.add(linha);
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

    private boolean ehLinhaVazia(String[] valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
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
