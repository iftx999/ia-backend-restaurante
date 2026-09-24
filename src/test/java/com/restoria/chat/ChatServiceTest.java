package com.restoria.chat;

import com.restoria.assinatura.LimiteUsoService;
import com.restoria.chat.dto.ChatRequest;
import com.restoria.chat.dto.ChatResponse;
import com.restoria.conhecimento.ConhecimentoService;
import com.restoria.conhecimento.TrechoRelevante;
import com.restoria.integration.ai.AiConsultantClient;
import com.restoria.integration.ai.AiConsultantException;
import com.restoria.integration.ai.AiMensagem;
import com.restoria.integration.ai.RespostaIaStreamListener;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversaChatRepository conversaChatRepository;

    @Autowired
    private MensagemChatRepository mensagemChatRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private AiConsultantClient aiConsultantClient;

    @MockBean
    @Qualifier("gpt")
    private AiConsultantClient gptConsultantClient;

    @MockBean
    private ConhecimentoService conhecimentoService;

    @MockBean
    private UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @Autowired
    private LimiteUsoService limiteUsoService;

    private Usuario usuarioFake;

    @BeforeEach
    void setUpUsuarioFake() {
        usuarioFake = usuarioRepository.save(
                new Usuario("Gerente Teste", "gerente-" + System.nanoTime() + "@teste.com", "hash", "Restaurante Teste"));
        when(usuarioAutenticadoProvider.obterAtual()).thenReturn(usuarioFake);
    }

    @Test
    void primeiraMensagemCriaConversaEPersisteAsDuasMensagens() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta da IA");

        ChatResponse resposta = chatService.responder(new ChatRequest(null, "Como calculo o CMV?"));

        assertThat(resposta.conversationId()).isNotBlank();
        assertThat(resposta.resposta()).isEqualTo("Resposta da IA");

        Long conversaId = Long.valueOf(resposta.conversationId());
        assertThat(conversaChatRepository.findById(conversaId)).isPresent();

        List<MensagemChat> mensagens = mensagemChatRepository.findByConversaIdOrderByEnviadaEmAsc(conversaId);
        assertThat(mensagens).hasSize(2);
        assertThat(mensagens.get(0).getAutor()).isEqualTo(AutorMensagem.USUARIO);
        assertThat(mensagens.get(0).getConteudo()).isEqualTo("Como calculo o CMV?");
        assertThat(mensagens.get(1).getAutor()).isEqualTo(AutorMensagem.IA);
        assertThat(mensagens.get(1).getConteudo()).isEqualTo("Resposta da IA");
    }

    @Test
    void segundaMensagemNaMesmaConversaReutilizaHistoricoEEnviaParaIa() {
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Primeira resposta");
        ChatResponse primeira = chatService.responder(new ChatRequest(null, "Primeira pergunta"));

        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Segunda resposta");
        ChatResponse segunda = chatService.responder(new ChatRequest(primeira.conversationId(), "Segunda pergunta"));

        assertThat(segunda.conversationId()).isEqualTo(primeira.conversationId());

        ArgumentCaptor<List<AiMensagem>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiConsultantClient, org.mockito.Mockito.times(2)).enviarMensagem(anyString(), captor.capture());

        List<AiMensagem> historicoEnviadoNaSegundaChamada = captor.getAllValues().get(1);
        assertThat(historicoEnviadoNaSegundaChamada).hasSize(3);
        assertThat(historicoEnviadoNaSegundaChamada.get(2).conteudo()).isEqualTo("Segunda pergunta");

        List<MensagemChat> mensagens = mensagemChatRepository
                .findByConversaIdOrderByEnviadaEmAsc(Long.valueOf(primeira.conversationId()));
        assertThat(mensagens).hasSize(4);
    }

    @Test
    void modeloIaGptRoteiaParaClientDoGptEEcoaNaResposta() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(gptConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta do GPT");

        ChatResponse resposta = chatService.responder(new ChatRequest(null, "Pergunta qualquer", null, null, "gpt"));

        assertThat(resposta.resposta()).isEqualTo("Resposta do GPT");
        assertThat(resposta.modeloIa()).isEqualTo("gpt");
        verify(aiConsultantClient, org.mockito.Mockito.never()).enviarMensagem(anyString(), anyList());
    }

    @Test
    void modeloIaAusenteRoteiaParaClaudePorPadrao() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta do Claude");

        ChatResponse resposta = chatService.responder(new ChatRequest(null, "Pergunta qualquer"));

        assertThat(resposta.resposta()).isEqualTo("Resposta do Claude");
        assertThat(resposta.modeloIa()).isEqualTo("claude");
        verify(gptConsultantClient, org.mockito.Mockito.never()).enviarMensagem(anyString(), anyList());
    }

    @Test
    void conversationIdInexistenteLancaExcecao() {
        org.junit.jupiter.api.Assertions.assertThrows(
                ConversaNaoEncontradaException.class,
                () -> chatService.responder(new ChatRequest("999999", "Pergunta qualquer")));
    }

    @Test
    void conversaDeOutroUsuarioLancaConversaNaoEncontrada() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta da IA");

        ChatResponse conversaDoUsuarioFake = chatService.responder(new ChatRequest(null, "Pergunta do usuario fake"));

        Usuario outroUsuario = usuarioRepository.save(
                new Usuario("Outro Gerente", "outro-" + System.nanoTime() + "@teste.com", "hash", "Outro Restaurante"));
        when(usuarioAutenticadoProvider.obterAtual()).thenReturn(outroUsuario);

        org.junit.jupiter.api.Assertions.assertThrows(
                ConversaNaoEncontradaException.class,
                () -> chatService.responder(new ChatRequest(conversaDoUsuarioFake.conversationId(), "Pergunta de outro usuario")));
    }

    @Test
    void systemPromptContemTrechosDaBaseDeConhecimentoQuandoRagRetornaResultado() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of(
                new TrechoRelevante("Mantenha o CMV abaixo de 30%.", "Boas praticas de CMV")));
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta da IA");

        chatService.responder(new ChatRequest(null, "Como reduzo o CMV?"));

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiConsultantClient).enviarMensagem(systemPromptCaptor.capture(), anyList());

        String systemPromptEnviado = systemPromptCaptor.getValue();
        assertThat(systemPromptEnviado).contains(RestoriaSystemPrompt.TEXTO);
        assertThat(systemPromptEnviado).contains("Mantenha o CMV abaixo de 30%.");
    }

    @Test
    void systemPromptPermaneceOriginalQuandoRagNaoRetornaTrechos() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta da IA");

        chatService.responder(new ChatRequest(null, "Como calculo margem de um prato?"));

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiConsultantClient).enviarMensagem(systemPromptCaptor.capture(), anyList());

        assertThat(systemPromptCaptor.getValue()).isEqualTo(RestoriaSystemPrompt.TEXTO);
    }

    @Test
    void responderStreamNotificaConversaIniciadaAntesDosTokensERepassaTokens() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            RespostaIaStreamListener streamListener = invocation.getArgument(2);
            streamListener.onToken("Ola");
            streamListener.onToken(" mundo");
            streamListener.onConcluido("Ola mundo");
            return null;
        }).when(aiConsultantClient).enviarMensagemStream(anyString(), anyList(), any(RespostaIaStreamListener.class));

        List<String> eventos = new ArrayList<>();
        List<String> tokensRecebidos = new ArrayList<>();
        List<String> conversaIds = new ArrayList<>();

        chatService.responderStream(new ChatRequest(null, "Como calculo o CMV?"), new ChatStreamListener() {
            @Override
            public void onConversaIniciada(String conversationId) {
                eventos.add("start");
                conversaIds.add(conversationId);
            }

            @Override
            public void onToken(String textoParcial) {
                eventos.add("token");
                tokensRecebidos.add(textoParcial);
            }

            @Override
            public void onConcluido() {
                eventos.add("done");
            }

            @Override
            public void onErro(Throwable erro) {
                eventos.add("error");
            }
        });

        assertThat(eventos).containsExactly("start", "token", "token", "done");
        assertThat(tokensRecebidos).containsExactly("Ola", " mundo");
        assertThat(conversaIds.get(0)).isNotBlank();

        Long conversaId = Long.valueOf(conversaIds.get(0));
        List<MensagemChat> mensagens = mensagemChatRepository.findByConversaIdOrderByEnviadaEmAsc(conversaId);
        assertThat(mensagens).hasSize(2);
        assertThat(mensagens.get(1).getAutor()).isEqualTo(AutorMensagem.IA);
        assertThat(mensagens.get(1).getConteudo()).isEqualTo("Ola mundo");
    }

    @Test
    void responderStreamSoPersisteMensagemDaIaQuandoStreamConcluiComSucesso() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            RespostaIaStreamListener streamListener = invocation.getArgument(2);
            streamListener.onToken("Parcial");
            // onConcluido deliberadamente nao chamado - simula stream ainda em andamento
            return null;
        }).when(aiConsultantClient).enviarMensagemStream(anyString(), anyList(), any(RespostaIaStreamListener.class));

        List<String> conversaIds = new ArrayList<>();

        chatService.responderStream(new ChatRequest(null, "Como calculo margem?"), new ChatStreamListener() {
            @Override
            public void onConversaIniciada(String conversationId) {
                conversaIds.add(conversationId);
            }

            @Override
            public void onToken(String textoParcial) {
                // nao usado neste teste
            }

            @Override
            public void onConcluido() {
                // nao deve ser chamado neste cenario
            }

            @Override
            public void onErro(Throwable erro) {
                // nao usado neste teste
            }
        });

        Long conversaId = Long.valueOf(conversaIds.get(0));
        List<MensagemChat> mensagens = mensagemChatRepository.findByConversaIdOrderByEnviadaEmAsc(conversaId);
        assertThat(mensagens).hasSize(1);
        assertThat(mensagens.get(0).getAutor()).isEqualTo(AutorMensagem.USUARIO);
    }

    @Test
    void responderStreamPropagaErroDoStreamListener() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        RuntimeException falha = new RuntimeException("falha na anthropic");
        doAnswer(invocation -> {
            RespostaIaStreamListener streamListener = invocation.getArgument(2);
            streamListener.onErro(falha);
            return null;
        }).when(aiConsultantClient).enviarMensagemStream(anyString(), anyList(), any(RespostaIaStreamListener.class));

        List<Throwable> errosRecebidos = new ArrayList<>();

        chatService.responderStream(new ChatRequest(null, "Pergunta qualquer"), new ChatStreamListener() {
            @Override
            public void onConversaIniciada(String conversationId) {
            }

            @Override
            public void onToken(String textoParcial) {
            }

            @Override
            public void onConcluido() {
            }

            @Override
            public void onErro(Throwable erro) {
                errosRecebidos.add(erro);
            }
        });

        assertThat(errosRecebidos).containsExactly(falha);
    }

    @Test
    void historicoEnviadoAIaEhLimitadoAJanelaEComecaComMensagemDoUsuario() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(aiConsultantClient.enviarMensagem(anyString(), anyList())).thenReturn("Resposta da IA");

        ConversaChat conversa = conversaChatRepository.save(new ConversaChat(usuarioFake, "Pergunta 0"));
        LocalDateTime inicio = LocalDateTime.now().minusHours(1);
        for (int i = 0; i < 25; i++) {
            salvarMensagem(conversa, AutorMensagem.USUARIO, "Pergunta " + i, inicio.plusSeconds(i * 2L));
            salvarMensagem(conversa, AutorMensagem.IA, "Resposta " + i, inicio.plusSeconds(i * 2L + 1));
        }

        chatService.responder(new ChatRequest(conversa.getId().toString(), "Pergunta nova"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMensagem>> historicoCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiConsultantClient).enviarMensagem(anyString(), historicoCaptor.capture());
        List<AiMensagem> historico = historicoCaptor.getValue();

        assertThat(historico).hasSizeLessThanOrEqualTo(ConversaHistoricoService.JANELA_HISTORICO + 1);
        assertThat(historico.get(0).role()).isEqualTo("user");
        assertThat(historico.get(historico.size() - 1).conteudo()).isEqualTo("Pergunta nova");
        assertThat(historico).extracting(AiMensagem::conteudo).contains("Resposta 24").doesNotContain("Pergunta 0");
    }

    @Test
    void falhaDaIaEstornaAReservaDeMensagem() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        when(aiConsultantClient.enviarMensagem(anyString(), anyList()))
                .thenThrow(new AiConsultantException("Anthropic fora do ar"));

        org.junit.jupiter.api.Assertions.assertThrows(AiConsultantException.class,
                () -> chatService.responder(new ChatRequest(null, "Como calculo o CMV?")));

        assertThat(limiteUsoService.resumoUso(usuarioFake).mensagensNoMes()).isZero();
    }

    @Test
    void responderStreamCanceladoPeloClientePersisteTextoParcial() {
        when(conhecimentoService.buscarTrechosRelevantes(anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            RespostaIaStreamListener streamListener = invocation.getArgument(2);
            streamListener.onToken("Resposta pela ");
            if (streamListener.cancelado()) {
                streamListener.onCancelado("Resposta pela ");
            }
            return null;
        }).when(aiConsultantClient).enviarMensagemStream(anyString(), anyList(), any(RespostaIaStreamListener.class));

        List<String> conversaIds = new ArrayList<>();
        chatService.responderStream(new ChatRequest(null, "Como calculo o CMV?"), new ChatStreamListener() {
            @Override
            public void onConversaIniciada(String conversationId) {
                conversaIds.add(conversationId);
            }

            @Override
            public void onToken(String textoParcial) {
            }

            @Override
            public void onConcluido() {
            }

            @Override
            public void onErro(Throwable erro) {
            }

            @Override
            public boolean cancelado() {
                return true;
            }
        });

        List<MensagemChat> mensagens = mensagemChatRepository
                .findByConversaIdOrderByEnviadaEmAsc(Long.valueOf(conversaIds.get(0)));
        assertThat(mensagens).extracting(MensagemChat::getAutor)
                .containsExactly(AutorMensagem.USUARIO, AutorMensagem.IA);
        assertThat(mensagens.get(1).getConteudo()).isEqualTo("Resposta pela ");
    }

    private void salvarMensagem(ConversaChat conversa, AutorMensagem autor, String conteudo, LocalDateTime enviadaEm) {
        MensagemChat mensagem = new MensagemChat(conversa, autor, conteudo);
        mensagem.setEnviadaEm(enviadaEm);
        mensagemChatRepository.save(mensagem);
    }
}
