package com.restoria.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Usuario do sistema (docs/03-modelo-dados.md). Autenticacao via login/senha +
 * JWT (RF-05) — ver pacote {@code com.restoria.security}.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nomeRestaurante;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean onboardingConcluido = false;

    /**
     * Limites usados por {@code IndicadorCalculator} para marcar anomalias
     * (RF-10). Configuraveis por usuario porque a tolerancia varia por tipo
     * de operacao (ex: rodizio tem perda de insumo naturalmente maior que
     * um bistro a la carte) — ver docs/04-roadmap.md.
     */
    @Column(nullable = false, columnDefinition = "numeric(5,4) default 0.20")
    private BigDecimal margemMinimaEsperada = new BigDecimal("0.20");

    @Column(nullable = false, columnDefinition = "numeric(5,4) default 0.05")
    private BigDecimal percentualPerdaAlerta = new BigDecimal("0.05");

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public Usuario(String nome, String email, String senha, String nomeRestaurante) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.nomeRestaurante = nomeRestaurante;
        this.criadoEm = LocalDateTime.now();
    }
}
