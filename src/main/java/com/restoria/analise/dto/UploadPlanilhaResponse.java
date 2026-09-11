package com.restoria.analise.dto;

import com.restoria.analise.StatusUpload;
import com.restoria.analise.TipoPlanilha;
import com.restoria.analise.UploadPlanilha;

public record UploadPlanilhaResponse(
        Long id,
        TipoPlanilha tipo,
        String nomeArquivoOriginal,
        StatusUpload status,
        String mensagemErro,
        int quantidadeLinhas
) {

    public static UploadPlanilhaResponse de(UploadPlanilha upload, int quantidadeLinhas) {
        return new UploadPlanilhaResponse(
                upload.getId(), upload.getTipo(), upload.getNomeArquivoOriginal(),
                upload.getStatus(), upload.getMensagemErro(), quantidadeLinhas);
    }
}
