package com.restoria.integration.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class StorageConfig {

    /** So existe quando S3_BUCKET esta configurado; sem ele, as imagens ficam em disco local. */
    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("!'${restoria.storage.s3.bucket:}'.isBlank()")
    S3Client s3Client(S3Properties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(properties.region()))
                .credentialsProvider(credenciais(properties));

        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint())).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnExpression("!'${restoria.storage.s3.bucket:}'.isBlank()")
    ArmazenamentoObjetos armazenamentoObjetos(S3Client s3Client, S3Properties properties) {
        return new S3ArmazenamentoObjetos(s3Client, properties.bucket());
    }

    private AwsCredentialsProvider credenciais(S3Properties properties) {
        if (properties.accessKeyId() == null || properties.accessKeyId().isBlank()) {
            return DefaultCredentialsProvider.builder().build();
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey()));
    }
}
