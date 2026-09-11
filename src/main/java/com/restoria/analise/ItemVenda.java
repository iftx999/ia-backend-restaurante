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
 * Linha extraida da planilha de vendas (RF-07, docs/03-modelo-dados.md).
 *
 * <p>{@code custoUnitario} nao estava no rascunho original do modelo de
 * dados: sem uma ficha tecnica (prato -> insumos) ainda nao implementada
 * (ver "Perguntas em aberto" em docs/03-modelo-dados.md), nao ha como
 * calcular margem por prato (RF-09) a partir so de quantidade/preco de
 * venda. Assumimos que a planilha de vendas do gerente ja traz o custo
 * unitario de cada prato (pratica comum de controle de CMV em restaurantes
 * pequenos); coluna opcional — quando ausente, indicadores de margem por
 * prato nao sao calculados para aquela linha.
 */
@Entity
@Table(name = "item_venda")
@Getter
@Setter
@NoArgsConstructor
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private UploadPlanilha upload;

    @Column(nullable = false)
    private String nomePrato;

    @Column(nullable = false)
    private Integer quantidadeVendida;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(precision = 12, scale = 2)
    private BigDecimal custoUnitario;

    private LocalDate data;

    public ItemVenda(UploadPlanilha upload, String nomePrato, Integer quantidadeVendida,
                      BigDecimal precoVenda, BigDecimal custoUnitario, LocalDate data) {
        this.upload = upload;
        this.nomePrato = nomePrato;
        this.quantidadeVendida = quantidadeVendida;
        this.precoVenda = precoVenda;
        this.custoUnitario = custoUnitario;
        this.data = data;
    }
}
