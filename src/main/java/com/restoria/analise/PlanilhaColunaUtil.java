package com.restoria.analise;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Helpers de leitura tolerante de colunas de planilha: aceita alguns nomes
 * alternativos de coluna e formatos numericos/de data comuns em planilhas
 * brasileiras (RF-13 - validar e orientar o usuario em caso de erro).
 */
final class PlanilhaColunaUtil {

    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    );

    private PlanilhaColunaUtil() {
    }

    static String texto(Map<String, String> linha, String... nomesPossiveis) {
        for (String nome : nomesPossiveis) {
            String valor = linha.get(nome);
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }

    /**
     * Aceita tanto "1234.56" / "1.234.56" (separador decimal ponto, formato
     * internacional/Excel en-US) quanto "1.234,56" (separador decimal
     * virgula, formato brasileiro). So tratamos "." como separador de
     * milhar quando a virgula tambem aparece — do contrario um valor comum
     * como "45.00" seria lido como 4500 em vez de 45.
     */
    static BigDecimal numero(Map<String, String> linha, String... nomesPossiveis) {
        String valor = texto(linha, nomesPossiveis);
        if (valor == null) {
            return null;
        }
        String normalizado = valor.contains(",")
                ? valor.replace(".", "").replace(",", ".")
                : valor;
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            throw new PlanilhaInvalidaException("Valor numerico invalido: \"" + valor + "\"");
        }
    }

    static Integer inteiro(Map<String, String> linha, String... nomesPossiveis) {
        BigDecimal valor = numero(linha, nomesPossiveis);
        return valor == null ? null : valor.intValue();
    }

    static LocalDate data(Map<String, String> linha, String... nomesPossiveis) {
        String valor = texto(linha, nomesPossiveis);
        if (valor == null) {
            return null;
        }
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                return LocalDate.parse(valor, formato);
            } catch (DateTimeParseException ignored) {
                // tenta o proximo formato
            }
        }
        throw new PlanilhaInvalidaException("Data invalida: \"" + valor + "\" (use dd/MM/yyyy ou yyyy-MM-dd)");
    }
}
