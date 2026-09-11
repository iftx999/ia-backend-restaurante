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
import java.time.LocalDate;

/**
 * Linha extraida da planilha de estoque/insumos (RF-08, docs/03-modelo-dados.md).
 */
@Entity
@Table(name = "item_estoque")
@Getter
@Setter
@NoArgsConstructor
public class ItemEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private UploadPlanilha upload;

    @Column(nullable = false)
    private String nomeInsumo;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeComprada;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal custoUnitario;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantidadePerdida;

    private LocalDate data;

    public ItemEstoque(UploadPlanilha upload, String nomeInsumo, BigDecimal quantidadeComprada,
                        BigDecimal custoUnitario, BigDecimal quantidadePerdida, LocalDate data) {
        this.upload = upload;
        this.nomeInsumo = nomeInsumo;
        this.quantidadeComprada = quantidadeComprada;
        this.custoUnitario = custoUnitario;
        this.quantidadePerdida = quantidadePerdida;
        this.data = data;
    }
}
