package com.restoria.chat;

import com.restoria.assinatura.LimiteUsoExcedidoException;
import com.restoria.chat.dto.ChatRequest;
import com.restoria.chat.dto.ChatResponse;
import com.restoria.chat.dto.ConversaDetalheResponse;
import com.restoria.chat.dto.ConversaResumoResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse enviarMensagem(@Valid @RequestBody ChatRequest request) {
        return chatService.responder(request);
    }

    /** Lista as conversas do usuario autenticado, mais recente primeiro (sidebar do chat). */
    @GetMapping("/conversas")
    public List<ConversaResumoResponse> listarConversas() {
        return chatService.listarConversas();
    }

    /** Mensagens de uma conversa, usado para reconstruir a tela apos um refresh da pagina. */
    @GetMapping("/conversas/{id}")
    public ConversaDetalheResponse buscarConversa(@PathVariable Long id) {
        return chatService.buscarConversa(id);
    }

    /**
     * Versao em streaming (SSE) do modo consultivo. Contrato:
     *
     * <p>Request: mesmo {@link ChatRequest} do {@code POST /api/chat}
     * ({@code conversationId}, {@code mensagem}).
     *
     * <p>Response: {@code text/event-stream}, com os seguintes eventos, nesta ordem:
     * <ul>
     *   <li>{@code event: start} — {@code data: {"conversationId": "<id>"}} — emitido assim
     *       que a conversa e resolvida/criada, antes de qualquer token.</li>
     *   <li>{@code event: token} — {@code data: {"texto": "<pedaco de texto>"}} — um evento
     *       por pedaco de texto que chega da IA (podem ser varios).</li>
     *   <li>{@code event: done} — {@code data: {}} — emitido quando a resposta termina e ja
     *       foi persistida no banco.</li>
     *   <li>{@code event: error} — {@code data: {"mensagem": "<mensagem de erro amigavel>", "tipo": "<tipo>"}} —
     *       emitido em vez do {@code done} se algo falhar em qualquer ponto. {@code tipo} e
     *       {@code "limite_uso"} quando o usuario estourou a cota do plano (frontend mostra
     *       CTA de upgrade) ou {@code "geral"} pra qualquer outra falha.</li>
     * </ul>
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter enviarMensagemStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);

        executor.execute(() -> {
            try {
                chatService.responderStream(request, new ChatStreamListener() {
                    @Override
                    public void onConversaIniciada(String conversationId) {
                        enviarEvento(emitter, "start", Map.of("conversationId", conversationId));
                    }

                    @Override
                    public void onToken(String textoParcial) {
                        enviarEvento(emitter, "token", Map.of("texto", textoParcial));
                    }

                    @Override
                    public void onConcluido() {
                        enviarEvento(emitter, "done", Map.of());
                        emitter.complete();
                    }

                    @Override
                    public void onErro(Throwable erro) {
                        finalizarComErro(emitter, request.conversationId(), erro);
                    }
                });
            } catch (Exception e) {
                finalizarComErro(emitter, request.conversationId(), e);
            }
        });

        return emitter;
    }

    private void enviarEvento(SseEmitter emitter, String nome, Object dados) {
        try {
            emitter.send(SseEmitter.event().name(nome).data(dados, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.complete();
        }
    }

    /**
     * Ja comunicamos a falha ao cliente via um evento SSE "error" proprio
     * (dados do nosso contrato, nao um erro HTTP). Por isso finalizamos com
     * {@code emitter.complete()} em vez de {@code completeWithError(erro)}:
     * completeWithError aciona a pagina de erro interna do Servlet/Spring
     * Security num redespacho assincrono sem contexto de autenticacao valido,
     * o que gera um AuthorizationDeniedException nos logs mesmo com o evento
     * ja entregue corretamente ao cliente.
     */
    private void finalizarComErro(SseEmitter emitter, String conversationId, Throwable erro) {
        log.warn("Falha ao gerar resposta em streaming para conversationId={}: {}",
                conversationId, erro.getMessage(), erro);

        // Limite de uso e um erro "esperado" de negocio (nao uma falha da IA): o
        // frontend usa "tipo" pra distinguir e mostrar o CTA de upgrade certo.
        boolean limiteExcedido = erro instanceof LimiteUsoExcedidoException;
        String mensagem = limiteExcedido ? erro.getMessage() : "Nao foi possivel obter resposta da IA no momento";

        enviarEvento(emitter, "error", Map.of("mensagem", mensagem, "tipo", limiteExcedido ? "limite_uso" : "geral"));
        emitter.complete();
    }
}
