package com.restoria.imagem;

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
 * Imagem de prato gerada/editada via IA (RF geração de imagem, plano PRO —
 * ver docs/06-geracao-imagem-ia.md). O binário fica em disco (ver
 * {@link ImagemStorageService}); aqui so a referencia e os metadados.
 */
@Entity
@Table(name = "imagem_prato")
@Getter
@Setter
@NoArgsConstructor
public class ImagemPrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOperacaoImagem tipoOperacao;

    /** Nulo se {@link TipoOperacaoImagem#GERACAO}; caminho da foto anexada se {@link TipoOperacaoImagem#EDICAO}. */
    private String imagemOriginalCaminho;

    @Column(nullable = false)
    private String caminhoArquivo;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    public ImagemPrato(Usuario usuario, String prompt, TipoOperacaoImagem tipoOperacao, String caminhoArquivo) {
        this.usuario = usuario;
        this.prompt = prompt;
        this.tipoOperacao = tipoOperacao;
        this.caminhoArquivo = caminhoArquivo;
        this.criadaEm = LocalDateTime.now();
    }
}
