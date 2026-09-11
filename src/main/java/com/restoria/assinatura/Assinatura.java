package com.restoria.assinatura;

import com.restoria.shared.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Assinatura do usuario (1:1 com {@link Usuario}). Criada automaticamente no
 * registro (ver {@code AuthService.registrar}), sempre comecando em
 * {@link Plano#GRATIS}/{@link StatusAssinatura#ATIVA} — todo usuario nasce
 * com uma assinatura, nunca fica sem.
 */
@Entity
@Table(name = "assinatura")
@Getter
@Setter
@NoArgsConstructor
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAssinatura status;

    /** Nulo ate o usuario iniciar um checkout do Stripe pela primeira vez. */
    private String stripeCustomerId;

    /** Nulo enquanto estiver no plano gratis (sem assinatura paga ativa). */
    private String stripeSubscriptionId;

    private LocalDateTime renovaEm;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    public Assinatura(Usuario usuario) {
        this.usuario = usuario;
        this.plano = Plano.GRATIS;
        this.status = StatusAssinatura.ATIVA;
        this.criadaEm = LocalDateTime.now();
    }
}
