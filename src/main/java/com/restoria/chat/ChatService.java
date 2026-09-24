package com.restoria.chat;

import com.restoria.assinatura.LimiteUsoService;
import com.restoria.assinatura.TipoUso;
import com.restoria.chat.dto.ChatRequest;
import com.restoria.chat.dto.ChatResponse;
import com.restoria.chat.dto.ConversaDetalheResponse;
import com.restoria.chat.dto.ConversaResumoResponse;
import com.restoria.integration.ai.AiConsultantClient;
import com.restoria.integration.ai.AiConsultantClientRouter;
import com.restoria.integration.ai.ModeloIa;
import com.restoria.integration.ai.RespostaIaStreamListener;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra o modo consultivo (RF-01/02/03/04).
 *
 * Historico persistido em ConversaChat/MensagemChat, associado ao usuario
 * autenticado da requisicao (RF-05). Acesso a banco delegado a
 * {@link ConversaHistoricoService}. Provedor de IA (Claude/GPT, RF-22)
 * resolvido por requisicao via {@link AiConsultantClientRouter}.
 */
@Service
public class ChatService {

    private final AiConsultantClientRouter aiConsultantClientRouter;
    private final ConversaHistoricoService conversaHistoricoService;
    private final LimiteUsoService limiteUsoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public ChatService(
            AiConsultantClientRouter aiConsultantClientRouter,
            ConversaHistoricoService conversaHistoricoService,
            LimiteUsoService limiteUsoService,
            UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.aiConsultantClientRouter = aiConsultantClientRouter;
        this.conversaHistoricoService = conversaHistoricoService;
        this.limiteUsoService = limiteUsoService;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    /**
     * Sem {@code @Transactional} de ponta a ponta: a chamada a IA pode levar
     * dezenas de segundos e nao deve prender uma conexao do pool do HikariCP.
     * Cada acesso a banco ({@link ConversaHistoricoService},
     * {@link LimiteUsoService}) abre sua propria transacao curta.
     */
    public ChatResponse responder(ChatRequest request) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        ModeloIa modelo = ModeloIa.normalizar(request.modeloIa());
        AiConsultantClient aiConsultantClient = aiConsultantClientRouter.resolver(modelo);

        limiteUsoService.reservar(usuario, TipoUso.MENSAGEM);
        ConversaHistoricoService.PreparacaoConversa preparacao;
        String resposta;
        try {
            String systemPrompt = conversaHistoricoService.montarSystemPromptComContexto(request.mensagem());
            preparacao = conversaHistoricoService.prepararConversaEHistorico(request, systemPrompt);
            resposta = aiConsultantClient.enviarMensagem(preparacao.systemPrompt(), preparacao.historico());
        } catch (RuntimeException e) {
            limiteUsoService.estornar(usuario, TipoUso.MENSAGEM);
            throw e;
        }

        conversaHistoricoService.persistirRespostaIa(preparacao.conversa().getId(), resposta);

        return new ChatResponse(preparacao.conversa().getId().toString(), resposta, modelo.name().toLowerCase());
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
     *
     * <p>Se o cliente desconectar ({@link ChatStreamListener#cancelado()}), a
     * leitura do provedor para e o texto parcial recebido e persistido.
     */
    public void responderStream(ChatRequest request, ChatStreamListener listener) {
        Usuario usuario;
        ConversaHistoricoService.PreparacaoConversa preparacao;
        AiConsultantClient aiConsultantClient;
        try {
            usuario = usuarioAutenticadoProvider.obterAtual();
            aiConsultantClient = aiConsultantClientRouter.resolver(ModeloIa.normalizar(request.modeloIa()));
            limiteUsoService.reservar(usuario, TipoUso.MENSAGEM);
        } catch (RuntimeException e) {
            listener.onErro(e);
            return;
        }

        try {
            String systemPrompt = conversaHistoricoService.montarSystemPromptComContexto(request.mensagem());
            preparacao = conversaHistoricoService.prepararConversaEHistorico(request, systemPrompt);
        } catch (RuntimeException e) {
            limiteUsoService.estornar(usuario, TipoUso.MENSAGEM);
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
                limiteUsoService.estornar(usuario, TipoUso.MENSAGEM);
                listener.onErro(erro);
            }

            @Override
            public boolean cancelado() {
                return listener.cancelado();
            }

            @Override
            public void onCancelado(String textoParcial) {
                // Tokens ja foram consumidos no provedor: a reserva nao e estornada.
                if (!textoParcial.isBlank()) {
                    conversaHistoricoService.persistirRespostaIa(preparacao.conversa().getId(), textoParcial);
                }
            }
        });
    }
}
