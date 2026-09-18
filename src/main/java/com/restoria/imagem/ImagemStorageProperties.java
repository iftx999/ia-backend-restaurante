package com.restoria.imagem;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.imagem` do application.yml.
 *
 * @param storageDir diretorio (relativo ao working dir do processo, ou absoluto) onde as
 *                    imagens geradas/editadas sao salvas em disco (ver docs/06-geracao-imagem-ia.md,
 *                    secao 3.3 — opcao A, disco local; migrar pra object storage quando o deploy
 *                    virar multi-instancia).
 */
@ConfigurationProperties(prefix = "restoria.imagem")
public record ImagemStorageProperties(
        @DefaultValue("uploads/imagens-prato") String storageDir
) {
}
