package com.restoria.conhecimento;

import com.restoria.integration.ai.EmbeddingClient;
import com.restoria.integration.ai.TipoEmbedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingestao e busca por similaridade da base de conhecimento (RAG, RF-19/RF-20).
 *
 * O RAG e um enriquecimento aditivo do modo consultivo: qualquer falha aqui
 * (embedding indisponivel, base vazia) deve degradar graciosamente para lista
 * vazia, sem quebrar o {@code ChatService}.
 */
@Service
public class ConhecimentoService {

    private static final Logger log = LoggerFactory.getLogger(ConhecimentoService.class);

    /** Tamanho alvo (em caracteres) de cada trecho ao quebrar um documento. */
    private static final int TAMANHO_MAXIMO_TRECHO = 1000;

    private final EmbeddingClient embeddingClient;
    private final DocumentoConhecimentoRepository documentoConhecimentoRepository;
    private final TrechoConhecimentoRepository trechoConhecimentoRepository;

    public ConhecimentoService(
            EmbeddingClient embeddingClient,
            DocumentoConhecimentoRepository documentoConhecimentoRepository,
            TrechoConhecimentoRepository trechoConhecimentoRepository) {
        this.embeddingClient = embeddingClient;
        this.documentoConhecimentoRepository = documentoConhecimentoRepository;
        this.trechoConhecimentoRepository = trechoConhecimentoRepository;
    }

    /**
     * Quebra o conteudo em trechos, gera o embedding de cada um (em lote) e
     * persiste o documento e seus trechos.
     */
    @Transactional
    public DocumentoConhecimento ingerir(String titulo, String conteudo, String fonte) {
        List<String> trechosTexto = dividirEmTrechos(conteudo);

        DocumentoConhecimento documento = new DocumentoConhecimento(titulo, fonte);
        documento = documentoConhecimentoRepository.save(documento);

        if (trechosTexto.isEmpty()) {
            return documento;
        }

        List<List<Float>> embeddings = embeddingClient.gerarEmbeddings(trechosTexto, TipoEmbedding.DOCUMENTO);

        for (int i = 0; i < trechosTexto.size(); i++) {
            float[] embedding = paraArray(embeddings.get(i));
            TrechoConhecimento trecho = new TrechoConhecimento(documento, i, trechosTexto.get(i), embedding);
            trechoConhecimentoRepository.save(trecho);
        }

        return documento;
    }

    /**
     * Busca os `k` trechos mais relevantes para a pergunta informada.
     * Retorna lista vazia (sem lancar excecao) se a base estiver vazia ou se
     * a geracao do embedding falhar.
     */
    @Transactional(readOnly = true)
    public List<TrechoRelevante> buscarTrechosRelevantes(String pergunta, int k) {
        if (pergunta == null || pergunta.isBlank()) {
            return List.of();
        }

        List<Float> embeddingPergunta;
        try {
            embeddingPergunta = embeddingClient.gerarEmbeddings(List.of(pergunta), TipoEmbedding.CONSULTA).get(0);
        } catch (Exception e) {
            log.warn("Falha ao gerar embedding da pergunta para busca RAG — seguindo sem contexto adicional: {}",
                    e.getMessage());
            return List.of();
        }

        try {
            String embeddingTexto = paraLiteralPgVector(embeddingPergunta);
            List<TrechoConhecimento> trechos = trechoConhecimentoRepository.buscarMaisSimilares(embeddingTexto, k);
            return trechos.stream()
                    .map(t -> new TrechoRelevante(t.getConteudo(), t.getDocumento().getTitulo()))
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao buscar trechos relevantes na base de conhecimento — seguindo sem contexto "
                    + "adicional: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Chunking simples por paragrafo: agrupa paragrafos consecutivos ate
     * aproximar o limite de {@link #TAMANHO_MAXIMO_TRECHO} caracteres.
     */
    List<String> dividirEmTrechos(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }

        String[] paragrafos = texto.strip().split("\\n\\s*\\n");
        List<String> trechos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();

        for (String paragrafoBruto : paragrafos) {
            String paragrafo = paragrafoBruto.strip();
            if (paragrafo.isEmpty()) {
                continue;
            }

            if (!atual.isEmpty() && atual.length() + paragrafo.length() + 2 > TAMANHO_MAXIMO_TRECHO) {
                trechos.add(atual.toString());
                atual.setLength(0);
            }

            if (!atual.isEmpty()) {
                atual.append("\n\n");
            }
            atual.append(paragrafo);
        }

        if (!atual.isEmpty()) {
            trechos.add(atual.toString());
        }

        return trechos;
    }

    private float[] paraArray(List<Float> valores) {
        float[] array = new float[valores.size()];
        for (int i = 0; i < valores.size(); i++) {
            array[i] = valores.get(i);
        }
        return array;
    }

    private String paraLiteralPgVector(List<Float> valores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(valores.get(i));
        }
        return sb.append(']').toString();
    }
}
