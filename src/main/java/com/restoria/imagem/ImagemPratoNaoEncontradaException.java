package com.restoria.imagem;

public class ImagemPratoNaoEncontradaException extends RuntimeException {

    public ImagemPratoNaoEncontradaException(Long id) {
        super("Imagem " + id + " nao encontrada");
    }
}
