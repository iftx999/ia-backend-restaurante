package com.restoria.conhecimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Documento de origem ingerido na base de conhecimento (RF-19), quebrado em
 * {@link TrechoConhecimento} para busca por similaridade (RAG).
 */
@Entity
@Table(name = "documento_conhecimento")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoConhecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    /** Origem/nome do arquivo original, opcional. */
    private String fonte;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public DocumentoConhecimento(String titulo, String fonte) {
        this.titulo = titulo;
        this.fonte = fonte;
        this.criadoEm = LocalDateTime.now();
    }
}
