package com.restoria.chat;

import com.restoria.assinatura.LimiteUsoService;
import com.restoria.chat.dto.ChatRequest;
import com.restoria.chat.dto.ChatResponse;
import com.restoria.chat.dto.ConversaDetalheResponse;
import com.restoria.chat.dto.ConversaResumoResponse;
import com.restoria.integration.ai.AiConsultantClient;
import com.restoria.integration.ai.RespostaIaStreamListener;
import com.restoria.security.UsuarioAutenticadoProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestra o modo consultivo (RF-01/02/03/04).
 *
 * Historico persistido em ConversaChat/MensagemChat, associado ao usuario
 * autenticado da requisicao (RF-05). Acesso a banco delegado a
 * {@link ConversaHistoricoService}.
 */
@Service
public class ChatService {

    private final AiConsultantClient aiConsultantClient;
    private final ConversaHistoricoService conversaHistoricoService;
    private final LimiteUsoService limiteUsoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public ChatService(
            AiConsultantClient aiConsultantClient,
            ConversaHistoricoService conversaHistoricoService,
            LimiteUsoService limiteUsoService,
            UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.aiConsultantClient = aiConsultantClient;
        this.conversaHistoricoService = conversaHistoricoService;
        this.limiteUsoService = limiteUsoService;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    @Transactional
    public ChatResponse responder(ChatRequest request) {
        limiteUsoService.verificarLimiteMensagem(usuarioAutenticadoProvider.obterAtual());

        ConversaHistoricoService.PreparacaoConversa preparacao =
                conversaHistoricoService.prepararConversaEHistorico(request);

        String resposta = aiConsultantClient.enviarMensagem(preparacao.systemPrompt(), preparacao.historico());

        conversaHistoricoService.persistirRespostaIa(preparacao.conversa().getId(), resposta);

        return new ChatResponse(preparacao.conversa().getId().toString(), resposta);
    }

    /** Lista as conversas do usuario autenticado (sidebar do chat, apos um refresh da pagina). */
    public List<ConversaResumoResponse> listarConversas() {
        return conversaHistoricoService.listarConversas();
    }

    /** Mensagens de uma conversa especifica, para reconstruir a tela apos um refresh. */
    public ConversaDetalheResponse buscarConversa(Long conversaId) {
        return conversaHistoricoService.buscarConversa(conversaId);
    }

    /**
     * Versao em streaming de {@link #responder(ChatRequest)} (RF-01/02/03/04).
     * Nao e anotado com {@code @Transactional} de ponta a ponta: a chamada de
     * rede a Anthropic pode levar varios segundos, e manter uma conexao de
     * banco presa durante todo esse tempo esgotaria o pool do HikariCP. As
     * operacoes de banco ficam isoladas em {@link ConversaHistoricoService}
     * (cada uma com sua propria transacao curta) e a chamada de rede acontece
     * fora de qualquer transacao.
     */
    public void responderStream(ChatRequest request, ChatStreamListener listener) {
        ConversaHistoricoService.PreparacaoConversa preparacao;
        try {
            limiteUsoService.verificarLimiteMensagem(usuarioAutenticadoProvider.obterAtual());
            preparacao = conversaHistoricoService.prepararConversaEHistorico(request);
        } catch (RuntimeException e) {
            listener.onErro(e);
            return;
        }

        listener.onConversaIniciada(preparacao.conversa().getId().toString());

        aiConsultantClient.enviarMensagemStream(preparacao.systemPrompt(), preparacao.historico(), new RespostaIaStreamListener() {
            @Override
            public void onToken(String textoParcial) {
                listener.onToken(textoParcial);
            }

            @Override
            public void onConcluido(String textoCompleto) {
                conversaHistoricoService.persistirRespostaIa(preparacao.conversa().getId(), textoCompleto);
                listener.onConcluido();
            }

            @Override
            public void onErro(Throwable erro) {
                listener.onErro(erro);
            }
        });
    }
}
