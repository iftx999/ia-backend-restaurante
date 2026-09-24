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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Acesso a banco do modo consultivo (resolver conversa, montar historico,
 * persistir mensagens). Isolado num bean proprio (em vez de metodos privados
 * em {@link ChatService}) porque {@code @Transactional} so funciona em
 * chamadas externas atraves do proxy do Spring — um metodo privado chamado
 * via {@code this} (auto-invocacao) ignora a anotacao silenciosamente.
 *
 * <p>Nenhum metodo {@code @Transactional} daqui faz chamada HTTP: a busca RAG
 * (embedding na Voyage AI) fica em {@link #montarSystemPromptComContexto},
 * que roda fora de transacao, para nao prender conexoes do pool.
 */
@Component
class ConversaHistoricoService {

    private final ConversaChatRepository conversaChatRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final ConhecimentoService conhecimentoService;

    private static final int QUANTIDADE_TRECHOS_CONTEXTO = 3;

    /**
     * Quantas mensagens anteriores da conversa vao para a IA. Sem limite, o
     * custo e a latencia cresceriam a cada turno ate estourar a janela de
     * contexto do modelo.
     */
    static final int JANELA_HISTORICO = 20;

    /** ~5MB de imagem decodificada (base64 e ~33% maior que os bytes originais). */
    private static final int TAMANHO_MAX_IMAGEM_BASE64 = 7_000_000;

    private static final List<String> MEDIA_TYPES_IMAGEM_SUPORTADOS =
            List.of("image/jpeg", "image/png", "image/gif", "image/webp");

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
     * historico (as ultimas {@link #JANELA_HISTORICO} mensagens) numa unica
     * transacao curta. O {@code systemPrompt} ja vem pronto de
     * {@link #montarSystemPromptComContexto}, chamado antes, fora da transacao.
     */
    @Transactional
    PreparacaoConversa prepararConversaEHistorico(ChatRequest request, String systemPrompt) {
        AiMensagem mensagemAtual = mensagemAtualDoUsuario(request);
        ConversaChat conversa = resolverConversa(request.conversationId(), request.mensagem());

        List<AiMensagem> historico = ultimasMensagens(conversa.getId());
        historico.add(mensagemAtual);
        mensagemChatRepository.save(new MensagemChat(conversa, AutorMensagem.USUARIO, request.mensagem()));

        return new PreparacaoConversa(conversa, historico, systemPrompt);
    }

    /**
     * Janela das ultimas mensagens em ordem cronologica. Se o corte cair numa
     * resposta da IA, ela e descartada: a API da Anthropic exige que a
     * primeira mensagem seja do usuario.
     */
    private List<AiMensagem> ultimasMensagens(Long conversaId) {
        List<MensagemChat> recentes = new ArrayList<>(mensagemChatRepository
                .findByConversaIdOrderByEnviadaEmDesc(conversaId, PageRequest.of(0, JANELA_HISTORICO)));
        Collections.reverse(recentes);

        while (!recentes.isEmpty() && recentes.get(0).getAutor() == AutorMensagem.IA) {
            recentes.remove(0);
        }

        return recentes.stream()
                .map(this::paraAiMensagem)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * A imagem anexada (se houver) so vale para o turno atual: nao e persistida
     * no historico (ver {@link MensagemChat}), so enviada a Anthropic junto com
     * a pergunta desta mensagem.
     */
    private AiMensagem mensagemAtualDoUsuario(ChatRequest request) {
        if (request.imagemBase64() == null || request.imagemBase64().isBlank()) {
            return AiMensagem.doUsuario(request.mensagem());
        }

        if (request.imagemMediaType() == null || !MEDIA_TYPES_IMAGEM_SUPORTADOS.contains(request.imagemMediaType())) {
            throw new ImagemInvalidaException(
                    "Formato de imagem nao suportado. Use JPEG, PNG, GIF ou WebP.");
        }

        if (request.imagemBase64().length() > TAMANHO_MAX_IMAGEM_BASE64) {
            throw new ImagemInvalidaException("Imagem muito grande (limite de ~5MB).");
        }

        return AiMensagem.doUsuarioComImagem(request.mensagem(), request.imagemBase64(), request.imagemMediaType());
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
                .map(conversa -> new ConversaResumoResponse(conversa.getId().toString(), tituloDaConversa(conversa)))
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

        return new ConversaDetalheResponse(conversa.getId().toString(), tituloDaConversa(conversa), mensagens);
    }

    private String tituloDaConversa(ConversaChat conversa) {
        return conversa.getTitulo() == null || conversa.getTitulo().isBlank() ? "Nova conversa" : conversa.getTitulo();
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
     *
     * <p>Faz chamada HTTP (embedding): chamar SEMPRE fora de transacao.
     */
    String montarSystemPromptComContexto(String pergunta) {
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

    private ConversaChat resolverConversa(String conversationId, String primeiraMensagem) {
        Usuario usuarioAtual = usuarioAutenticadoProvider.obterAtual();

        if (conversationId == null || conversationId.isBlank()) {
            return conversaChatRepository.save(new ConversaChat(usuarioAtual, primeiraMensagem));
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
