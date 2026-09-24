package com.restoria.assinatura;

/** Recursos com custo direto de IA contabilizados por mes em {@link UsoMensal}. */
public enum TipoUso {
    MENSAGEM,
    RELATORIO,
    IMAGEM;

    int limiteDo(Plano plano) {
        return switch (this) {
            case MENSAGEM -> plano.getMensagensPorMes();
            case RELATORIO -> plano.getRelatoriosPorMes();
            case IMAGEM -> plano.getImagensPorMes();
        };
    }
}
