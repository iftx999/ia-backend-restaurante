package com.restoria.integration.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiSseStreamProcessorTest {

    /**
     * Fixture com o formato de SSE da API de Responses da OpenAI (`stream:
     * true`), incluindo eventos de ciclo de vida que devem ser ignorados
     * (response.created, response.in_progress, response.output_item.added,
     * response.content_part.added, response.output_text.done,
     * response.content_part.done, response.output_item.done).
     */
    private static final String SSE_EXEMPLO = """
            event: response.created
            data: {"type": "response.created", "response": {"id": "resp_1", "status": "in_progress"}}

            event: response.in_progress
            data: {"type": "response.in_progress", "response": {"id": "resp_1", "status": "in_progress"}}

            event: response.output_item.added
            data: {"type": "response.output_item.added", "output_index": 0, "item": {"id": "msg_1", "type": "message"}}

            event: response.content_part.added
            data: {"type": "response.content_part.added", "item_id": "msg_1", "output_index": 0, "content_index": 0, "part": {"type": "output_text", "text": ""}}

            event: response.output_text.delta
            data: {"type": "response.output_text.delta", "item_id": "msg_1", "output_index": 0, "content_index": 0, "delta": "Hello"}

            event: response.output_text.delta
            data: {"type": "response.output_text.delta", "item_id": "msg_1", "output_index": 0, "content_index": 0, "delta": "!"}

            event: response.output_text.done
            data: {"type": "response.output_text.done", "item_id": "msg_1", "output_index": 0, "content_index": 0, "text": "Hello!"}

            event: response.content_part.done
            data: {"type": "response.content_part.done", "item_id": "msg_1", "output_index": 0, "content_index": 0}

            event: response.output_item.done
            data: {"type": "response.output_item.done", "output_index": 0, "item": {"id": "msg_1", "type": "message"}}

            event: response.completed
            data: {"type": "response.completed", "response": {"id": "resp_1", "status": "completed"}}

            """;

    @Test
    void processaTokensNaOrdemEChamaOnConcluidoComTextoCompleto() throws IOException {
        OpenAiSseStreamProcessor processor = new OpenAiSseStreamProcessor();
        List<String> tokensRecebidos = new ArrayList<>();
        List<String> textoCompletoRecebido = new ArrayList<>();
        List<Throwable> errosRecebidos = new ArrayList<>();

        processor.processar(new StringReader(SSE_EXEMPLO), new RespostaIaStreamListener() {
            @Override
            public void onToken(String textoParcial) {
                tokensRecebidos.add(textoParcial);
            }

            @Override
            public void onConcluido(String textoCompleto) {
                textoCompletoRecebido.add(textoCompleto);
            }

            @Override
            public void onErro(Throwable erro) {
                errosRecebidos.add(erro);
            }
        });

        assertThat(errosRecebidos).isEmpty();
        assertThat(tokensRecebidos).containsExactly("Hello", "!");
        assertThat(textoCompletoRecebido).containsExactly("Hello!");
    }

    @Test
    void eventoDeFalhaNoMeioDoStreamChamaOnErro() throws IOException {
        String sseComErro = """
                event: response.output_text.delta
                data: {"type": "response.output_text.delta", "item_id": "msg_1", "output_index": 0, "content_index": 0, "delta": "Ola"}

                event: response.failed
                data: {"type": "response.failed", "response": {"id": "resp_1", "status": "failed", "error": {"message": "API sobrecarregada"}}}

                """;

        OpenAiSseStreamProcessor processor = new OpenAiSseStreamProcessor();
        List<String> tokensRecebidos = new ArrayList<>();
        List<Throwable> errosRecebidos = new ArrayList<>();

        processor.processar(new StringReader(sseComErro), new RespostaIaStreamListener() {
            @Override
            public void onToken(String textoParcial) {
                tokensRecebidos.add(textoParcial);
            }

            @Override
            public void onConcluido(String textoCompleto) {
                // nao deve ser chamado
            }

            @Override
            public void onErro(Throwable erro) {
                errosRecebidos.add(erro);
            }
        });

        assertThat(tokensRecebidos).containsExactly("Ola");
        assertThat(errosRecebidos).hasSize(1);
        assertThat(errosRecebidos.get(0).getMessage()).contains("API sobrecarregada");
    }

    @Test
    void streamSemResponseCompletedChamaOnErro() throws IOException {
        String sseIncompleto = """
                event: response.output_text.delta
                data: {"type": "response.output_text.delta", "item_id": "msg_1", "output_index": 0, "content_index": 0, "delta": "Ola"}

                """;

        OpenAiSseStreamProcessor processor = new OpenAiSseStreamProcessor();
        List<Throwable> errosRecebidos = new ArrayList<>();

        processor.processar(new StringReader(sseIncompleto), new RespostaIaStreamListener() {
            @Override
            public void onToken(String textoParcial) {
            }

            @Override
            public void onConcluido(String textoCompleto) {
            }

            @Override
            public void onErro(Throwable erro) {
                errosRecebidos.add(erro);
            }
        });

        assertThat(errosRecebidos).hasSize(1);
        assertThat(errosRecebidos.get(0).getMessage()).contains("sem response.completed");
    }
}
