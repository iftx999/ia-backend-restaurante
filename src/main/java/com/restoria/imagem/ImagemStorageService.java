package com.restoria.imagem;

import com.restoria.integration.storage.ArmazenamentoObjetos;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Salva/le o binario das imagens de prato (ver docs/06-geracao-imagem-ia.md,
 * secao 3.3). Nomes de arquivo sao sempre gerados aqui (UUID), nunca
 * derivados de entrada do usuario — evita qualquer risco de path traversal.
 *
 * <p>Com object storage configurado (S3_BUCKET, ver
 * {@code integration.storage.StorageConfig}) grava la; sem ele, em disco
 * local. Em producao use sempre o object storage: o disco do container no
 * Railway e apagado a cada redeploy e nao e compartilhado entre instancias.
 */
@Component
@EnableConfigurationProperties(ImagemStorageProperties.class)
class ImagemStorageService {

    private final Path baseDir;
    private final ArmazenamentoObjetos armazenamentoObjetos;

    ImagemStorageService(ImagemStorageProperties properties, ObjectProvider<ArmazenamentoObjetos> armazenamentoObjetos) {
        this.baseDir = Path.of(properties.storageDir());
        this.armazenamentoObjetos = armazenamentoObjetos.getIfAvailable();
    }

    /** @return o caminho relativo salvo (persistido em {@link ImagemPrato#getCaminhoArquivo()}), nao o caminho absoluto. */
    String salvar(Long usuarioId, byte[] dados, String mediaType) {
        if (armazenamentoObjetos != null) {
            String chave = usuarioId + "/" + UUID.randomUUID() + "." + extensaoDe(mediaType);
            try {
                armazenamentoObjetos.gravar(chave, dados, mediaType);
                return chave;
            } catch (RuntimeException e) {
                throw new ImagemStorageException("Falha ao salvar imagem no object storage: " + e.getMessage(), e);
            }
        }

        try {
            Path diretorioUsuario = baseDir.resolve(String.valueOf(usuarioId));
            Files.createDirectories(diretorioUsuario);

            String nomeArquivo = UUID.randomUUID() + "." + extensaoDe(mediaType);
            Path arquivo = diretorioUsuario.resolve(nomeArquivo);
            Files.write(arquivo, dados);

            return usuarioId + "/" + nomeArquivo;
        } catch (IOException e) {
            throw new ImagemStorageException("Falha ao salvar imagem em disco: " + e.getMessage(), e);
        }
    }

    byte[] ler(String caminhoRelativo) {
        if (armazenamentoObjetos != null) {
            try {
                return armazenamentoObjetos.ler(caminhoRelativo);
            } catch (RuntimeException e) {
                throw new ImagemStorageException("Falha ao ler imagem do object storage: " + e.getMessage(), e);
            }
        }

        try {
            return Files.readAllBytes(baseDir.resolve(caminhoRelativo));
        } catch (IOException e) {
            throw new ImagemStorageException("Falha ao ler imagem do disco: " + e.getMessage(), e);
        }
    }

    private String extensaoDe(String mediaType) {
        if (mediaType == null) {
            return "png";
        }
        return switch (mediaType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "png";
        };
    }
}
