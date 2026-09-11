package com.restoria.chat;

import com.restoria.chat.dto.ChatRequest;
import com.restoria.chat.dto.ConversaDetalheResponse;
import com.restoria.chat.dto.ConversaResumoResponse;
import com.restoria.chat.dto.MensagemResponse;
import com.restoria.conhecimento.ConhecimentoService;
import com.restoria.conhecimento.TrechoRelevante;
import com.restoria.integration.ai.AiMensagem;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Acesso a banco do modo consultivo (resolver conversa, montar historico,
 * persistir mensagens). Isolado num bean proprio (em vez de metodos privados
 * em {@link ChatService}) porque {@code @Transactional} so funciona em
 * chamadas externas atraves do proxy do Spring — um metodo privado chamado
 * via {@code this} (auto-invocacao) ignora a anotacao silenciosamente. Isso
 * importava aqui porque {@code MensagemChat.conteudo} e um {@code @Lob}
 * (Postgres {@code oid}): ler seu conteudo fora de uma transacao ativa falha
 * com "Unable to access lob stream" — o que so acontecia a partir da segunda
 * mensagem de uma conversa (quando ha historico de fato para ler).
 */
@Component
class ConversaHistoricoService {

    private final ConversaChatRepository conversaChatRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final ConhecimentoService conhecimentoService;

    private static final int QUANTIDADE_TRECHOS_CONTEXTO = 3;

    ConversaHistoricoService(
            ConversaChatRepository conversaChatRepository,
            MensagemChatRepository mensagemChatRepository,
            UsuarioAutenticadoProvider usuarioAutenticadoProvider,
            ConhecimentoService conhecimentoService) {
        this.conversaChatRepository = conversaChatRepository;
        this.mensagemChatRepository = mensagemChatRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
        this.conhecimentoService = conhecimentoService;
    }

    /**
     * Resolve/cria a conversa, persiste a mensagem do usuario e monta o
     * historico + system prompt — tudo dentro de uma unica transacao (por
     * isso o historico pode ler {@code conteudo} das mensagens antigas sem
     * problema).
     */
    @Transactional
    PreparacaoConversa prepararConversaEHistorico(ChatRequest request) {
        ConversaChat conversa = resolverConversa(request.conversationId());

        List<AiMensagem> historico = mensagemChatRepository
                .findByConversaIdOrderByEnviadaEmAsc(conversa.getId())
                .stream()
                .map(this::paraAiMensagem)
                .collect(Collectors.toCollection(ArrayList::new));

        historico.add(AiMensagem.doUsuario(request.mensagem()));
        mensagemChatRepository.save(new MensagemChat(conversa, AutorMensagem.USUARIO, request.mensagem()));

        String systemPrompt = montarSystemPromptComContexto(request.mensagem());

        return new PreparacaoConversa(conversa, historico, systemPrompt);
    }

    /** Persiste a mensagem da IA (chamada depois que a resposta - streaming ou nao - termina). */
    @Transactional
    void persistirRespostaIa(Long conversaId, String textoCompleto) {
        ConversaChat conversa = conversaChatRepository.getReferenceById(conversaId);
        mensagemChatRepository.save(new MensagemChat(conversa, AutorMensagem.IA, textoCompleto));
    }

    /** Lista as conversas do usuario autenticado, mais recente primeiro (sidebar do chat). */
    @Transactional(readOnly = true)
    List<ConversaResumoResponse> listarConversas() {
        Usuario usuarioAtual = usuarioAutenticadoProvider.obterAtual();
        return conversaChatRepository.findByUsuarioIdOrderByIniciadaEmDesc(usuarioAtual.getId())
                .stream()
                .map(conversa -> new ConversaResumoResponse(conversa.getId().toString(), tituloDaConversa(conversa.getId())))
                .collect(Collectors.toList());
    }

    /** Mensagens de uma conversa, para reconstruir a tela apos um refresh. */
    @Transactional(readOnly = true)
    ConversaDetalheResponse buscarConversa(Long conversaId) {
        ConversaChat conversa = resolverConversaExistente(conversaId);

        List<MensagemResponse> mensagens = mensagemChatRepository
                .findByConversaIdOrderByEnviadaEmAsc(conversaId)
                .stream()
                .map(mensagem -> new MensagemResponse(
                        mensagem.getAutor() == AutorMensagem.USUARIO ? "user" : "assistant",
                        mensagem.getConteudo()))
                .collect(Collectors.toList());

        return new ConversaDetalheResponse(conversa.getId().toString(), tituloDaConversa(conversaId), mensagens);
    }

    private String tituloDaConversa(Long conversaId) {
        return mensagemChatRepository
                .findFirstByConversaIdAndAutorOrderByEnviadaEmAsc(conversaId, AutorMensagem.USUARIO)
                .map(MensagemChat::getConteudo)
                .orElse("Nova conversa");
    }

    private ConversaChat resolverConversaExistente(Long conversaId) {
        Usuario usuarioAtual = usuarioAutenticadoProvider.obterAtual();
        ConversaChat conversa = conversaChatRepository.findById(conversaId)
                .orElseThrow(() -> new ConversaNaoEncontradaException(conversaId));

        if (!conversa.getUsuario().getId().equals(usuarioAtual.getId())) {
            throw new ConversaNaoEncontradaException(conversaId);
        }

        return conversa;
    }

    /**
     * Enriquece o system prompt com trechos relevantes da base de conhecimento
     * (RAG, RF-20). Aditivo/opcional: se a busca nao retornar nada (base vazia
     * ou falha no embedding), o prompt original e usado sem alteracao.
     */
    private String montarSystemPromptComContexto(String pergunta) {
        List<TrechoRelevante> trechos = conhecimentoService.buscarTrechosRelevantes(
                pergunta, QUANTIDADE_TRECHOS_CONTEXTO);

        if (trechos.isEmpty()) {
            return RestoriaSystemPrompt.TEXTO;
        }

        StringBuilder contexto = new StringBuilder();
        contexto.append(RestoriaSystemPrompt.TEXTO);
        contexto.append("\n\nContexto adicional recuperado da base de conhecimento ")
                .append("(use como referencia, mas nao cite literalmente que \"veio de uma busca\"):\n");

        for (int i = 0; i < trechos.size(); i++) {
            contexto.append(i + 1).append(". ").append(trechos.get(i).conteudo()).append('\n');
        }

        return contexto.toString();
    }

    private ConversaChat resolverConversa(String conversationId) {
        Usuario usuarioAtual = usuarioAutenticadoProvider.obterAtual();

        if (conversationId == null || conversationId.isBlank()) {
            return conversaChatRepository.save(new ConversaChat(usuarioAtual));
        }

        Long id = parseConversationId(conversationId);
        ConversaChat conversa = conversaChatRepository.findById(id)
                .orElseThrow(() -> new ConversaNaoEncontradaException(id));

        if (!conversa.getUsuario().getId().equals(usuarioAtual.getId())) {
            throw new ConversaNaoEncontradaException(id);
        }

        return conversa;
    }

    private Long parseConversationId(String conversationId) {
        try {
            return Long.valueOf(conversationId);
        } catch (NumberFormatException e) {
            throw new ConversaNaoEncontradaException(conversationId);
        }
    }

    private AiMensagem paraAiMensagem(MensagemChat mensagem) {
        return mensagem.getAutor() == AutorMensagem.USUARIO
                ? AiMensagem.doUsuario(mensagem.getConteudo())
                : AiMensagem.daIa(mensagem.getConteudo());
    }

    record PreparacaoConversa(ConversaChat conversa, List<AiMensagem> historico, String systemPrompt) {
    }
}
