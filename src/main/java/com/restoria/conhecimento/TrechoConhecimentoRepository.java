package com.restoria.conhecimento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrechoConhecimentoRepository extends JpaRepository<TrechoConhecimento, Long> {

    /**
     * Busca os `k` trechos com embedding mais proximo (menor distancia de
     * cosseno, operador `<=>`) do embedding informado.
     *
     * Depende da extensao `pgvector` estar instalada no Postgres (o operador
     * `<=>` so existe apos `CREATE EXTENSION vector`) — isso nao e controlado
     * pelo Java, ver {@code PgVectorExtensionInitializer}.
     *
     * @param embeddingTexto vetor no formato de literal pgvector, ex: "[0.1,0.2,...]"
     */
    @Query(value = """
            SELECT t.* FROM trecho_conhecimento t
            ORDER BY t.embedding <=> CAST(:embeddingTexto AS vector)
            LIMIT :k
            """, nativeQuery = true)
    List<TrechoConhecimento> buscarMaisSimilares(@Param("embeddingTexto") String embeddingTexto, @Param("k") int k);

    long countByDocumentoId(Long documentoId);
}
