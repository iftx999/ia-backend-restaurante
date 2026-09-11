package com.restoria.analise;

public class RelatorioNaoEncontradoException extends RuntimeException {

    public RelatorioNaoEncontradoException(Long id) {
        super("Relatorio nao encontrado: " + id);
    }
}
