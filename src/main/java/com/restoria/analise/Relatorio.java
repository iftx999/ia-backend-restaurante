package com.restoria.analise;

import com.restoria.shared.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Relatorio analitico gerado a partir de uploads de vendas/estoque
 * (RF-09/RF-10/RF-11, docs/03-modelo-dados.md).
 */
@Entity
@Table(name = "relatorio")
@Getter
@Setter
@NoArgsConstructor
public class Relatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_vendas_id")
    private UploadPlanilha uploadVendas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_estoque_id")
    private UploadPlanilha uploadEstoque;

    @Column(precision = 7, scale = 4)
    private BigDecimal cmvCalculado;

    /**
     * Texto normal (nao {@code @Lob}) de proposito: {@code @Lob} numa String
     * mapeia para {@code oid} no Postgres, que so pode ser lido com o stream
     * do large object aberto (transacao ativa) — mesmo problema documentado
     * em {@code MensagemChat.conteudo}. Aqui o relatorio e lido e usado fora
     * de uma transacao (export de PDF/Excel, resposta do controller), entao
     * uma coluna {@code text} comum (sem limite pratico de tamanho no
     * Postgres) evita a armadilha.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String conteudoTextoIA;

    @Column(nullable = false)
    private LocalDateTime geradoEm;

    /**
     * RF-16: true se algum indicador (margem de prato ou perda de insumo)
     * saiu da faixa esperada do usuario. Usado pelo frontend pra mostrar um
     * banner de alerta proativo no chat sem o usuario precisar abrir o
     * relatorio (ver {@code AnaliseService.obterAlertaMaisRecente}).
     */
    @Column(nullable = false)
    private boolean temAlerta;

    /** Quantas anomalias (RF-10) foram detectadas neste relatorio — usado no texto do banner. */
    @Column(nullable = false)
    private int quantidadeAlertas;

    @OneToMany(mappedBy = "relatorio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndicadorPrato> indicadoresPrato = new ArrayList<>();

    public Relatorio(Usuario usuario, UploadPlanilha uploadVendas, UploadPlanilha uploadEstoque,
                      BigDecimal cmvCalculado, String conteudoTextoIA) {
        this.usuario = usuario;
        this.uploadVendas = uploadVendas;
        this.uploadEstoque = uploadEstoque;
        this.cmvCalculado = cmvCalculado;
        this.conteudoTextoIA = conteudoTextoIA;
        this.geradoEm = LocalDateTime.now();
    }

    public void adicionarIndicadorPrato(IndicadorPrato indicador) {
        indicador.setRelatorio(this);
        indicadoresPrato.add(indicador);
    }
}
