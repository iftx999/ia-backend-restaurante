package com.restoria.assinatura;

/**
 * Planos disponiveis. Limites hardcoded aqui (em vez de tabela + admin UI)
 * porque na V1 sao só dois planos fixos — evita complexidade desnecessaria
 * antes de validar que o modelo de cobranca faz sentido.
 */
public enum Plano {

    GRATIS(20, 5),
    PRO(500, 100);

    private final int mensagensPorMes;
    private final int relatoriosPorMes;

    Plano(int mensagensPorMes, int relatoriosPorMes) {
        this.mensagensPorMes = mensagensPorMes;
        this.relatoriosPorMes = relatoriosPorMes;
    }

    public int getMensagensPorMes() {
        return mensagensPorMes;
    }

    public int getRelatoriosPorMes() {
        return relatoriosPorMes;
    }
}
