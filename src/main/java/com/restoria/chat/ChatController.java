package com.restoria.chat;

import com.restoria.assinatura.LimiteUsoExcedidoException;
import com.restoria.chat.dto.ChatRequest;
import com.restoria.chat.dto.ChatResponse;
import com.restoria.chat.dto.ConversaDetalheResponse;
import com.restoria.chat.dto.ConversaResumoResponse;
import com.restoria.integration.ai.IaSobrecarregadaException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    /**
     * Virtual thread por stream. O SecurityContext da requisicao e copiado
     * explicitamente para a task (DelegatingSecurityContextExecutorService):
     * o ChatService precisa do usuario autenticado nessa outra thread. Antes
     * isso era feito com MODE_INHERITABLETHREADLOCAL global, que tambem
     * vazava o login do usuario para qualquer thread de pool criada durante
     * uma requisicao.
     */
    private final ExecutorService executor =
            new DelegatingSecurityContextExecutorService(Executors.newVirtualThreadPerTaskExecutor());

    /**
     * Usuarios (e-mail do token) com um stream em andamento. Um stream por
     * usuario: evita que varias abas/scripts multipliquem as conexoes abertas
     * com o provedor de IA. Estado em memoria, por instancia — mesma ressalva
     * de {@code LoginRateLimiter} se o deploy virar multi-instancia.
     */
    private final Set<String> streamsAtivos = ConcurrentHashMap.newKeySet();

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
     *       CTA de upgrade), {@code "sobrecarga"} quando o provedor de IA esta no limite de
     *       chamadas simultaneas (temporario), {@code "stream_em_andamento"} quando o usuario
     *       ja tem outra resposta sendo gerada, ou {@code "geral"} pra qualquer outra falha.</li>
     * </ul>
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter enviarMensagemStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);

        // Cliente fechou a aba, caiu a rede ou estourou o timeout: sinaliza
        // para o ChatService parar de ler o provedor (e de gastar tokens).
        AtomicBoolean cancelado = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelado.set(true));
        emitter.onTimeout(() -> cancelado.set(true));
        emitter.onError(erro -> cancelado.set(true));

        String chaveUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!streamsAtivos.add(chaveUsuario)) {
            enviarEvento(emitter, cancelado, "error", Map.of(
                    "mensagem", "Aguarde a resposta anterior terminar antes de enviar outra mensagem.",
                    "tipo", "stream_em_andamento"));
            emitter.complete();
            return emitter;
        }

        executor.execute(() -> {
            try {
                chatService.responderStream(request, new ChatStreamListener() {
                    @Override
                    public void onConversaIniciada(String conversationId) {
                        enviarEvento(emitter, cancelado, "start", Map.of("conversationId", conversationId));
                    }

                    @Override
                    public void onToken(String textoParcial) {
                        enviarEvento(emitter, cancelado, "token", Map.of("texto", textoParcial));
                    }

                    @Override
                    public void onConcluido() {
                        enviarEvento(emitter, cancelado, "done", Map.of());
                        concluir(emitter);
                    }

                    @Override
                    public void onErro(Throwable erro) {
                        finalizarComErro(emitter, cancelado, request.conversationId(), erro);
                    }

                    @Override
                    public boolean cancelado() {
                        return cancelado.get();
                    }
                });
            } catch (Exception e) {
                finalizarComErro(emitter, cancelado, request.conversationId(), e);
            } finally {
                streamsAtivos.remove(chaveUsuario);
            }
        });

        return emitter;
    }

    private void enviarEvento(SseEmitter emitter, AtomicBoolean cancelado, String nome, Object dados) {
        if (cancelado.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(nome).data(dados, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            // Cliente desconectou. Nao chamar complete() aqui: o container ja
            // encerrou o AsyncContext e recusaria com IllegalStateException,
            // que escaparia do callback de token e viraria um "erro" do stream.
            cancelado.set(true);
        }
    }

    /** complete() tolerante a stream ja encerrado pelo container (cliente desconectado). */
    private void concluir(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("SseEmitter ja encerrado pelo container: {}", e.getMessage());
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
    private void finalizarComErro(SseEmitter emitter, AtomicBoolean cancelado, String conversationId, Throwable erro) {
        log.warn("Falha ao gerar resposta em streaming para conversationId={}: {}",
                conversationId, erro.getMessage(), erro);

        // Limite de uso e um erro "esperado" de negocio (nao uma falha da IA): o
        // frontend usa "tipo" pra distinguir e mostrar o CTA de upgrade certo.
        // IA sobrecarregada (bulkhead cheio) tambem tem mensagem propria: e
        // temporario, o usuario so precisa tentar de novo em instantes.
        String tipo;
        String mensagem;
        if (erro instanceof LimiteUsoExcedidoException) {
            tipo = "limite_uso";
            mensagem = erro.getMessage();
        } else if (erro instanceof IaSobrecarregadaException) {
            tipo = "sobrecarga";
            mensagem = erro.getMessage();
        } else {
            tipo = "geral";
            mensagem = "Nao foi possivel obter resposta da IA no momento";
        }

        enviarEvento(emitter, cancelado, "error", Map.of("mensagem", mensagem, "tipo", tipo));
        concluir(emitter);
    }
}
