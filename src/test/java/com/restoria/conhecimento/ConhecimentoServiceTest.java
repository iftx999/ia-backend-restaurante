package com.restoria.conhecimento;

import com.restoria.integration.ai.EmbeddingClient;
import com.restoria.integration.ai.TipoEmbedding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConhecimentoServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private DocumentoConhecimentoRepository documentoConhecimentoRepository;

    @Mock
    private TrechoConhecimentoRepository trechoConhecimentoRepository;

    private ConhecimentoService conhecimentoService;

    private ConhecimentoService novoService() {
        return new ConhecimentoService(embeddingClient, documentoConhecimentoRepository, trechoConhecimentoRepository,
                TransactionOperations.withoutTransaction());
    }

    // --- dividirEmTrechos (chunking) ---

    @Test
    void textoVazioNaoGeraTrechos() {
        conhecimentoService = novoService();

        assertThat(conhecimentoService.dividirEmTrechos(null)).isEmpty();
        assertThat(conhecimentoService.dividirEmTrechos("")).isEmpty();
        assertThat(conhecimentoService.dividirEmTrechos("   ")).isEmpty();
    }

    @Test
    void textoCurtoGeraUmUnicoTrecho() {
        conhecimentoService = novoService();

        String texto = "Um paragrafo curto sobre gestao de estoque de restaurante.";

        List<String> trechos = conhecimentoService.dividirEmTrechos(texto);

        assertThat(trechos).hasSize(1);
        assertThat(trechos.get(0)).isEqualTo(texto);
    }

    @Test
    void textoLongoEQuebradoEmMultiplosTrechos() {
        conhecimentoService = novoService();

        String paragrafo = "x".repeat(600);
        // 4 paragrafos de 600 chars, separados por linha em branco: nao cabem
        // todos no limite de 1000 chars por trecho, entao devem virar mais de 1 trecho.
        String texto = String.join("\n\n", paragrafo, paragrafo, paragrafo, paragrafo);

        List<String> trechos = conhecimentoService.dividirEmTrechos(texto);

        assertThat(trechos.size()).isGreaterThan(1);
        trechos.forEach(t -> assertThat(t).isNotBlank());
        // nenhum conteudo de paragrafo e perdido no processo
        assertThat(String.join("", trechos)).contains(paragrafo);
    }

    // --- buscarTrechosRelevantes ---

    @Test
    void buscarTrechosRelevantesRetornaVazioSeEmbeddingClientFalhar() {
        conhecimentoService = novoService();

        when(embeddingClient.gerarEmbeddings(anyList(), any(TipoEmbedding.class)))
                .thenThrow(new RuntimeException("Voyage AI indisponivel"));

        List<TrechoRelevante> resultado = conhecimentoService.buscarTrechosRelevantes("Como reduzir o CMV?", 3);

        assertThat(resultado).isEmpty();
        verify(trechoConhecimentoRepository, never()).buscarMaisSimilares(anyString(), anyInt());
    }

    @Test
    void buscarTrechosRelevantesRetornaVazioParaPerguntaEmBranco() {
        conhecimentoService = novoService();

        List<TrechoRelevante> resultado = conhecimentoService.buscarTrechosRelevantes("   ", 3);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarTrechosRelevantesRetornaTrechosQuandoBuscaFunciona() {
        conhecimentoService = novoService();

        when(embeddingClient.gerarEmbeddings(anyList(), any(TipoEmbedding.class)))
                .thenReturn(List.of(List.of(0.1f, 0.2f, 0.3f)));

        DocumentoConhecimento documento = new DocumentoConhecimento("Boas praticas de CMV", "manual.txt");
        TrechoConhecimento trecho = new TrechoConhecimento(documento, 0, "Mantenha o CMV abaixo de 30%.",
                new float[] {0.1f, 0.2f, 0.3f});

        when(trechoConhecimentoRepository.buscarMaisSimilares(anyString(), anyInt()))
                .thenReturn(List.of(trecho));

        List<TrechoRelevante> resultado = conhecimentoService.buscarTrechosRelevantes("Como reduzir o CMV?", 3);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).conteudo()).isEqualTo("Mantenha o CMV abaixo de 30%.");
        assertThat(resultado.get(0).tituloDocumento()).isEqualTo("Boas praticas de CMV");
    }
}
