package com.restoria.integration.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicSseStreamProcessorTest {

    /**
     * Fixture com o formato exato de SSE devolvido pela API da Anthropic
     * (ver docs oficiais de streaming), incluindo eventos que devem ser
     * ignorados (message_start, content_block_start, ping, content_block_stop,
     * message_delta).
     */
    private static final String SSE_EXEMPLO = """
            event: message_start
            data: {"type": "message_start", "message": {"id": "msg_1", "type": "message", "role": "assistant", "content": [], "model": "claude-3-5-sonnet-20241022", "usage": {"input_tokens": 10, "output_tokens": 1}}}

            event: content_block_start
            data: {"type": "content_block_start", "index": 0, "content_block": {"type": "text", "text": ""}}

            event: ping
            data: {"type": "ping"}

            event: content_block_delta
            data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "Hello"}}

            event: content_block_delta
            data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "!"}}

            event: content_block_stop
            data: {"type": "content_block_stop", "index": 0}

            event: message_delta
            data: {"type": "message_delta", "delta": {"stop_reason": "end_turn", "stop_sequence": null}, "usage": {"output_tokens": 15}}

            event: message_stop
            data: {"type": "message_stop"}

            """;

    @Test
    void processaTokensNaOrdemEChamaOnConcluidoComTextoCompleto() throws IOException {
        AnthropicSseStreamProcessor processor = new AnthropicSseStreamProcessor();
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
    void eventoDeErroNoMeioDoStreamChamaOnErro() throws IOException {
        String sseComErro = """
                event: content_block_delta
                data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "Ola"}}

                event: error
                data: {"type": "error", "error": {"type": "overloaded_error", "message": "API sobrecarregada"}}

                """;

        AnthropicSseStreamProcessor processor = new AnthropicSseStreamProcessor();
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
    void paraDeLerQuandoOClienteCancelaERepassaOTextoParcial() throws IOException {
        AnthropicSseStreamProcessor processor = new AnthropicSseStreamProcessor();
        List<String> tokensRecebidos = new ArrayList<>();
        List<String> textoParcialRecebido = new ArrayList<>();
        List<String> eventosFinais = new ArrayList<>();

        processor.processar(new StringReader(SSE_EXEMPLO), new RespostaIaStreamListener() {
            @Override
            public void onToken(String textoParcial) {
                tokensRecebidos.add(textoParcial);
            }

            @Override
            public void onConcluido(String textoCompleto) {
                eventosFinais.add("concluido");
            }

            @Override
            public void onErro(Throwable erro) {
                eventosFinais.add("erro");
            }

            @Override
            public boolean cancelado() {
                return !tokensRecebidos.isEmpty();
            }

            @Override
            public void onCancelado(String textoParcial) {
                textoParcialRecebido.add(textoParcial);
            }
        });

        assertThat(tokensRecebidos).containsExactly("Hello");
        assertThat(textoParcialRecebido).containsExactly("Hello");
        assertThat(eventosFinais).isEmpty();
    }
}
