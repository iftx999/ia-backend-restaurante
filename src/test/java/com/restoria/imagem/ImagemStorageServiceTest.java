package com.restoria.imagem;

import com.restoria.integration.storage.ArmazenamentoObjetos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemStorageServiceTest {

    @TempDir
    Path diretorio;

    @Test
    void semObjectStorageGravaELeDoDiscoLocal() throws Exception {
        ImagemStorageService service = new ImagemStorageService(
                new ImagemStorageProperties(diretorio.toString()), provedor(null));

        String caminho = service.salvar(7L, new byte[] {1, 2, 3}, "image/jpeg");

        assertThat(caminho).startsWith("7/").endsWith(".jpg");
        assertThat(Files.exists(diretorio.resolve(caminho))).isTrue();
        assertThat(service.ler(caminho)).containsExactly(1, 2, 3);
    }

    @Test
    void comObjectStorageGravaELeDoBucketENaoTocaNoDisco() {
        ArmazenamentoEmMemoria bucket = new ArmazenamentoEmMemoria();
        ImagemStorageService service = new ImagemStorageService(
                new ImagemStorageProperties(diretorio.toString()), provedor(bucket));

        String caminho = service.salvar(7L, new byte[] {4, 5}, "image/png");

        assertThat(bucket.objetos).containsKey(caminho);
        assertThat(bucket.mediaTypes.get(caminho)).isEqualTo("image/png");
        assertThat(service.ler(caminho)).containsExactly(4, 5);
        assertThat(diretorio.resolve("7")).doesNotExist();
    }

    @Test
    void falhaDoObjectStorageViraImagemStorageException() {
        ArmazenamentoObjetos quebrado = new ArmazenamentoObjetos() {
            @Override
            public void gravar(String chave, byte[] dados, String mediaType) {
                throw new IllegalStateException("bucket inacessivel");
            }

            @Override
            public byte[] ler(String chave) {
                throw new IllegalStateException("bucket inacessivel");
            }
        };
        ImagemStorageService service = new ImagemStorageService(
                new ImagemStorageProperties(diretorio.toString()), provedor(quebrado));

        assertThatThrownBy(() -> service.salvar(7L, new byte[] {1}, "image/png"))
                .isInstanceOf(ImagemStorageException.class)
                .hasMessageContaining("bucket inacessivel");
    }

    private ObjectProvider<ArmazenamentoObjetos> provedor(ArmazenamentoObjetos armazenamento) {
        StaticListableBeanFactory fabrica = new StaticListableBeanFactory();
        if (armazenamento != null) {
            fabrica.addBean("armazenamentoObjetos", armazenamento);
        }
        return fabrica.getBeanProvider(ArmazenamentoObjetos.class);
    }

    private static class ArmazenamentoEmMemoria implements ArmazenamentoObjetos {
        final Map<String, byte[]> objetos = new HashMap<>();
        final Map<String, String> mediaTypes = new HashMap<>();

        @Override
        public void gravar(String chave, byte[] dados, String mediaType) {
            objetos.put(chave, dados);
            mediaTypes.put(chave, mediaType);
        }

        @Override
        public byte[] ler(String chave) {
            return objetos.get(chave);
        }
    }
}
