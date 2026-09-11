package com.restoria.assinatura.dto;

public record AssinaturaResponse(
        String plano,
        String status,
        long mensagensUsadasNoMes,
        int mensagensLimiteNoMes,
        long relatoriosUsadosNoMes,
        int relatoriosLimiteNoMes
) {
}
