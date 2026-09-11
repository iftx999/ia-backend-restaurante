package com.restoria.analise;

public class UploadNaoEncontradoException extends RuntimeException {

    public UploadNaoEncontradoException(Long id) {
        super("Upload de planilha nao encontrado: " + id);
    }
}
