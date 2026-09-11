package com.restoria.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Faz o parsing manual do formato SSE (Server-Sent Events) devolvido pela API
 * da Anthropic quando `stream: true` e' enviado (ver docs oficiais de
 * streaming da Anthropic). So' processa os eventos relevantes para um chat
 * somente-texto: `content_block_delta` (delta do tipo `text_delta`) e
 * `message_stop`. Os demais eventos (`message_start`, `content_block_start`,
 * `ping`, `content_block_stop`, `message_delta`) sao ignorados. Um
 * `event: error` no meio do stream aciona {@link RespostaIaStreamListener#onErro}.
 *
 * Extraido como classe separada (sem depender de rede) para poder ser testado
 * isoladamente com um SSE de exemplo como fixture.
 */
class AnthropicSseStreamProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Le o SSE bloco a bloco (blocos separados por linha em branco) e notifica
     * o listener. Encerra ao encontrar `message_stop` ou `error`, ou ao chegar
     * ao fim do stream sem um desses eventos (tratado como falha inesperada).
     */
    void processar(Reader reader, RespostaIaStreamListener listener) throws IOException {
        BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        StringBuilder textoCompleto = new StringBuilder();

        String eventoAtual = null;
        StringBuilder dadosAtuais = new StringBuilder();
        String linha;
        boolean concluido = false;

        while ((linha = bufferedReader.readLine()) != null) {
            if (linha.isBlank()) {
                if (eventoAtual != null && !dadosAtuais.isEmpty()) {
                    concluido = processarBloco(eventoAtual, dadosAtuais.toString(), textoCompleto, listener);
                }
                eventoAtual = null;
                dadosAtuais.setLength(0);

                if (concluido) {
                    return;
                }
                continue;
            }

            if (linha.startsWith("event:")) {
                eventoAtual = linha.substring("event:".length()).trim();
            } else if (linha.startsWith("data:")) {
                if (!dadosAtuais.isEmpty()) {
                    dadosAtuais.append('\n');
                }
                dadosAtuais.append(linha.substring("data:".length()).trim());
            }
        }

        listener.onErro(new AiConsultantException("Stream da API da Anthropic encerrado sem message_stop"));
    }

    /**
     * @return true se o stream deve ser encerrado (sucesso ou erro), false para continuar lendo.
     */
    private boolean processarBloco(String evento, String dados, StringBuilder textoCompleto, RespostaIaStreamListener listener) {
        try {
            JsonNode json = objectMapper.readTree(dados);

            switch (evento) {
                case "content_block_delta" -> {
                    JsonNode delta = json.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                        String texto = delta.path("text").asText("");
                        textoCompleto.append(texto);
                        listener.onToken(texto);
                    }
                    return false;
                }
                case "message_stop" -> {
                    listener.onConcluido(textoCompleto.toString());
                    return true;
                }
                case "error" -> {
                    String mensagemErro = json.path("error").path("message").asText("Erro desconhecido da API da Anthropic");
                    listener.onErro(new AiConsultantException("Erro no stream da API da Anthropic: " + mensagemErro));
                    return true;
                }
                default -> {
                    // message_start, content_block_start, ping, content_block_stop, message_delta: ignorados
                    return false;
                }
            }
        } catch (Exception e) {
            listener.onErro(new AiConsultantException("Falha ao parsear evento SSE da Anthropic: " + e.getMessage(), e));
            return true;
        }
    }
}
