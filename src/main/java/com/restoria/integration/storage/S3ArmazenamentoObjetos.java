package com.restoria.integration.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** {@link ArmazenamentoObjetos} sobre S3 ou qualquer provedor compativel (R2, MinIO). */
class S3ArmazenamentoObjetos implements ArmazenamentoObjetos {

    private final S3Client s3Client;
    private final String bucket;

    S3ArmazenamentoObjetos(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void gravar(String chave, byte[] dados, String mediaType) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(chave).contentType(mediaType).build(),
                RequestBody.fromBytes(dados));
    }

    @Override
    public byte[] ler(String chave) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(chave).build())
                .asByteArray();
    }
}
