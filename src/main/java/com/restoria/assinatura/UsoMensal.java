package com.restoria.assinatura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Contador de uso do mes (competencia "AAAA-MM") por usuario. E a fonte de
 * verdade do limite do plano: {@link LimiteUsoService} reserva uma unidade
 * ANTES da chamada a IA, com o usuario travado (SELECT ... FOR UPDATE), e
 * estorna se a chamada falhar. Contar direto nos timestamps de
 * MensagemChat/Relatorio (como era antes) deixava requisicoes simultaneas
 * passarem todas na verificacao e estourarem a cota.
 */
@Entity
@Table(name = "uso_mensal",
        uniqueConstraints = @UniqueConstraint(name = "uk_uso_mensal_usuario_competencia",
                columnNames = {"usuario_id", "competencia"}))
@Getter
@NoArgsConstructor
public class UsoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 7)
    private String competencia;

    @Column(nullable = false)
    private int mensagens;

    @Column(nullable = false)
    private int relatorios;

    @Column(nullable = false)
    private int imagens;

    public UsoMensal(Long usuarioId, String competencia) {
        this.usuarioId = usuarioId;
        this.competencia = competencia;
    }

    public int quantidade(TipoUso tipo) {
        return switch (tipo) {
            case MENSAGEM -> mensagens;
            case RELATORIO -> relatorios;
            case IMAGEM -> imagens;
        };
    }

    void somar(TipoUso tipo, int delta) {
        switch (tipo) {
            case MENSAGEM -> mensagens = Math.max(0, mensagens + delta);
            case RELATORIO -> relatorios = Math.max(0, relatorios + delta);
            case IMAGEM -> imagens = Math.max(0, imagens + delta);
        }
    }
}
