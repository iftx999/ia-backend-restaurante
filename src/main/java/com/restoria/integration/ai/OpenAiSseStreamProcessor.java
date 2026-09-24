package com.restoria.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Faz o parsing manual do formato SSE da API de Responses da OpenAI (`POST
 * /v1/responses` com `stream: true`). Espelha {@link AnthropicSseStreamProcessor}:
 * mesma estrutura de leitura bloco a bloco, mas eventos diferentes. Cada bloco
 * de dados ja traz um campo `type` proprio (a API tipa cada evento no corpo
 * JSON, nao so na linha `event:` do SSE), entao usamos esse campo em vez de
 * depender da linha `event:` do protocolo.
 *
 * <p>So processa os eventos relevantes para um chat somente-texto:
 * `response.output_text.delta` (delta incremental) e `response.completed`
 * (fim com sucesso). `response.failed`/`response.incomplete`/`error` viram
 * falha. Os demais eventos de ciclo de vida (`response.created`,
 * `response.in_progress`, `response.output_item.*`, `response.content_part.*`,
 * `response.output_text.done` etc.) sao ignorados.
 */
class OpenAiSseStreamProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    void processar(Reader reader, RespostaIaStreamListener listener) throws IOException {
        BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        StringBuilder textoCompleto = new StringBuilder();

        StringBuilder dadosAtuais = new StringBuilder();
        String linha;
        boolean concluido = false;

        while ((linha = bufferedReader.readLine()) != null) {
            if (listener.cancelado()) {
                listener.onCancelado(textoCompleto.toString());
                return;
            }

            if (linha.isBlank()) {
                if (!dadosAtuais.isEmpty()) {
                    concluido = processarBloco(dadosAtuais.toString(), textoCompleto, listener);
                }
                dadosAtuais.setLength(0);

                if (concluido) {
                    return;
                }
                continue;
            }

            if (linha.startsWith("data:")) {
                if (!dadosAtuais.isEmpty()) {
                    dadosAtuais.append('\n');
                }
                dadosAtuais.append(linha.substring("data:".length()).trim());
            }
            // Linha "event:" ignorada de proposito: o tipo do evento ja vem no JSON (campo "type").
        }

        listener.onErro(new AiConsultantException("Stream da API da OpenAI encerrado sem response.completed"));
    }

    /**
     * @return true se o stream deve ser encerrado (sucesso ou erro), false para continuar lendo.
     */
    private boolean processarBloco(String dados, StringBuilder textoCompleto, RespostaIaStreamListener listener) {
        try {
            JsonNode json = objectMapper.readTree(dados);
            String tipo = json.path("type").asText("");

            switch (tipo) {
                case "response.output_text.delta" -> {
                    String texto = json.path("delta").asText("");
                    textoCompleto.append(texto);
                    listener.onToken(texto);
                    return false;
                }
                case "response.completed" -> {
                    listener.onConcluido(textoCompleto.toString());
                    return true;
                }
                case "response.failed", "response.incomplete", "error" -> {
                    String mensagemErro = extrairMensagemErro(json);
                    listener.onErro(new AiConsultantException("Erro no stream da API da OpenAI: " + mensagemErro));
                    return true;
                }
                default -> {
                    // response.created, response.in_progress, response.output_item.*,
                    // response.content_part.*, response.output_text.done etc.: ignorados.
                    return false;
                }
            }
        } catch (Exception e) {
            listener.onErro(new AiConsultantException("Falha ao parsear evento SSE da OpenAI: " + e.getMessage(), e));
            return true;
        }
    }

    private String extrairMensagemErro(JsonNode json) {
        JsonNode mensagem = json.path("message");
        if (!mensagem.isMissingNode() && !mensagem.isNull()) {
            return mensagem.asText("Erro desconhecido da API da OpenAI");
        }
        return json.path("response").path("error").path("message").asText("Erro desconhecido da API da OpenAI");
    }
}
