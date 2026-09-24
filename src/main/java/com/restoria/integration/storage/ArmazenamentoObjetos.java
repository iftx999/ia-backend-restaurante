package com.restoria.integration.storage;

/**
 * Armazenamento de arquivos binarios fora do disco da instancia (object
 * storage). Necessario porque o disco do container no Railway e efemero
 * (apagado a cada redeploy) e nao e compartilhado entre instancias.
 */
public interface ArmazenamentoObjetos {

    void gravar(String chave, byte[] dados, String mediaType);

    byte[] ler(String chave);
}
