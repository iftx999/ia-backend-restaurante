package com.restoria.perfil.dto;

import com.restoria.shared.Usuario;

import java.math.BigDecimal;

public record PerfilResponse(
        String nome,
        String nomeRestaurante,
        boolean onboardingConcluido,
        BigDecimal margemMinimaEsperada,
        BigDecimal percentualPerdaAlerta
) {

    public static PerfilResponse de(Usuario usuario) {
        return new PerfilResponse(
                usuario.getNome(),
                usuario.getNomeRestaurante(),
                usuario.isOnboardingConcluido(),
                usuario.getMargemMinimaEsperada(),
                usuario.getPercentualPerdaAlerta());
    }
}
