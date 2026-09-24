package com.restoria.integration.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.storage.s3` do application.yml. Credenciais NUNCA
 * versionadas: sempre via variavel de ambiente (S3_ACCESS_KEY_ID /
 * S3_SECRET_ACCESS_KEY). Sem {@code bucket}, o object storage fica desligado
 * e as imagens continuam em disco local (dev).
 *
 * @param endpoint so para provedores S3-compativeis (ex: Cloudflare R2
 *                 {@code https://<account>.r2.cloudflarestorage.com}, MinIO);
 *                 vazio para a AWS.
 * @param region   {@code auto} no R2; regiao do bucket na AWS (ex: {@code sa-east-1}).
 */
@ConfigurationProperties(prefix = "restoria.storage.s3")
public record S3Properties(
        String bucket,
        String endpoint,
        @DefaultValue("auto") String region,
        String accessKeyId,
        String secretAccessKey
) {
}
