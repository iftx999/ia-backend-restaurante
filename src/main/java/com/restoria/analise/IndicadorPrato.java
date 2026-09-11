package com.restoria.analise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Indicador de margem calculado por prato, ligado a um {@link Relatorio}
 * (RF-09/RF-10, docs/03-modelo-dados.md).
 */
@Entity
@Table(name = "indicador_prato")
@Getter
@Setter
@NoArgsConstructor
public class IndicadorPrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relatorio_id", nullable = false)
    private Relatorio relatorio;

    @Column(nullable = false)
    private String nomePrato;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal margemCalculada;

    @Column(nullable = false)
    private boolean alerta;

    public IndicadorPrato(String nomePrato, BigDecimal margemCalculada, boolean alerta) {
        this.nomePrato = nomePrato;
        this.margemCalculada = margemCalculada;
        this.alerta = alerta;
    }
}
