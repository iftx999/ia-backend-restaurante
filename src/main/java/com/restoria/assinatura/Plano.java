package com.restoria.assinatura;

/**
 * Planos disponiveis. Limites hardcoded aqui (em vez de tabela + admin UI)
 * porque na V1 sao só dois planos fixos — evita complexidade desnecessaria
 * antes de validar que o modelo de cobranca faz sentido.
 *
 * <p>Geracao de imagem (docs/06-geracao-imagem-ia.md, secao 6) e exclusiva do
 * plano PRO: imagem custa mais por chamada que uma mensagem de chat, e a
 * decisao de produto foi nao incluir no plano gratis. {@code GRATIS} fica com
 * {@code imagensPorMes = 0}, o que bloqueia a feature inteiramente pra esse
 * plano (ver {@code LimiteUsoService.reservar}).
 */
public enum Plano {

    GRATIS(20, 5, 0),
    PRO(500, 100, 20);

    private final int mensagensPorMes;
    private final int relatoriosPorMes;
    private final int imagensPorMes;

    Plano(int mensagensPorMes, int relatoriosPorMes, int imagensPorMes) {
        this.mensagensPorMes = mensagensPorMes;
        this.relatoriosPorMes = relatoriosPorMes;
        this.imagensPorMes = imagensPorMes;
    }

    public int getMensagensPorMes() {
        return mensagensPorMes;
    }

    public int getRelatoriosPorMes() {
        return relatoriosPorMes;
    }

    public int getImagensPorMes() {
        return imagensPorMes;
    }
}
