package com.restoria.conhecimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Chunk (trecho) de um {@link DocumentoConhecimento}, com o embedding gerado
 * via Voyage AI ({@code integration.ai.EmbeddingClient}).
 *
 * A coluna `embedding` depende da extensao `pgvector` estar instalada no
 * Postgres (ver {@code PgVectorExtensionInitializer}) — isso e um
 * pre-requisito de infraestrutura, nao controlado pelo Java.
 */
@Entity
@Table(name = "trecho_conhecimento")
@Getter
@Setter
@NoArgsConstructor
public class TrechoConhecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoConhecimento documento;

    @Column(nullable = false)
    private Integer ordem;

    @Lob
    @Column(nullable = false)
    private String conteudo;

    @Column(columnDefinition = "vector(1024)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    private float[] embedding;

    public TrechoConhecimento(DocumentoConhecimento documento, Integer ordem, String conteudo, float[] embedding) {
        this.documento = documento;
        this.ordem = ordem;
        this.conteudo = conteudo;
        this.embedding = embedding;
    }
}
