package com.restoria.imagem;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Salva/le o binario das imagens de prato em disco local (ver
 * docs/06-geracao-imagem-ia.md, secao 3.3). Nomes de arquivo sao sempre
 * gerados aqui (UUID), nunca derivados de entrada do usuario — evita
 * qualquer risco de path traversal.
 */
@Component
@EnableConfigurationProperties(ImagemStorageProperties.class)
class ImagemStorageService {

    private final Path baseDir;

    ImagemStorageService(ImagemStorageProperties properties) {
        this.baseDir = Path.of(properties.storageDir());
    }

    /** @return o caminho relativo salvo (persistido em {@link ImagemPrato#getCaminhoArquivo()}), nao o caminho absoluto. */
    String salvar(Long usuarioId, byte[] dados, String mediaType) {
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
