package com.restoria.analise;

import java.util.List;

/**
 * Planilha enviada nao pode ser processada: formato invalido, colunas
 * obrigatorias ausentes ou vazias (RF-13). Pode carregar um erro por linha
 * problematica da planilha, para que o usuario corrija tudo de uma vez em vez
 * de descobrir um problema por upload.
 */
public class PlanilhaInvalidaException extends RuntimeException {

    private final List<String> erros;

    public PlanilhaInvalidaException(String mensagem) {
        this(List.of(mensagem));
    }

    public PlanilhaInvalidaException(List<String> erros) {
        super(String.join("; ", erros));
        this.erros = erros;
    }

    public List<String> getErros() {
        return erros;
    }
}
