package com.restoria.imagem;

class ImagemStorageException extends RuntimeException {
    ImagemStorageException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
