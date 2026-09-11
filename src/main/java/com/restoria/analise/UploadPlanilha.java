package com.restoria.analise;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Planilha de vendas/estoque enviada pelo usuario (RF-07/RF-08,
 * docs/03-modelo-dados.md).
 */
@Entity
@Table(name = "upload_planilha")
@Getter
@Setter
@NoArgsConstructor
public class UploadPlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPlanilha tipo;

    @Column(nullable = false)
    private String nomeArquivoOriginal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUpload status;

    private String mensagemErro;

    @Column(nullable = false)
    private LocalDateTime enviadoEm;

    public UploadPlanilha(Usuario usuario, TipoPlanilha tipo, String nomeArquivoOriginal) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.nomeArquivoOriginal = nomeArquivoOriginal;
        this.status = StatusUpload.PROCESSANDO;
        this.enviadoEm = LocalDateTime.now();
    }
}
