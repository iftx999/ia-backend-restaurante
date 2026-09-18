package com.restoria.assinatura;

import com.restoria.analise.RelatorioRepository;
import com.restoria.chat.AutorMensagem;
import com.restoria.chat.MensagemChatRepository;
import com.restoria.shared.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimiteUsoServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private MensagemChatRepository mensagemChatRepository;

    @Mock
    private RelatorioRepository relatorioRepository;

    private LimiteUsoService limiteUsoService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        limiteUsoService = new LimiteUsoService(assinaturaRepository, mensagemChatRepository, relatorioRepository);
        usuario = new Usuario();
        usuario.setId(1L);
    }

    @Test
    void permiteMensagemQuandoAbaixoDoLimiteDoPlanoGratis() {
        semAssinaturaAtiva();
        when(mensagemChatRepository.countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
                eq(1L), eq(AutorMensagem.USUARIO), any(), any())).thenReturn(19L);

        limiteUsoService.verificarLimiteMensagem(usuario);
    }

    @Test
    void bloqueiaMensagemQuandoAtingeLimiteDoPlanoGratis() {
        semAssinaturaAtiva();
        when(mensagemChatRepository.countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
                eq(1L), eq(AutorMensagem.USUARIO), any(), any())).thenReturn(20L);

        assertThatThrownBy(() -> limiteUsoService.verificarLimiteMensagem(usuario))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("20 mensagens");
    }

    @Test
    void bloqueiaRelatorioQuandoAtingeLimiteDoPlanoGratis() {
        semAssinaturaAtiva();
        when(relatorioRepository.countByUsuarioAndGeradoEmBetween(eq(usuario), any(), any())).thenReturn(5L);

        assertThatThrownBy(() -> limiteUsoService.verificarLimiteRelatorio(usuario))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("5 relatorios");
    }

    @Test
    void planoProAtivoUsaLimitesMaisAltos() {
        Assinatura assinatura = new Assinatura(usuario);
        assinatura.setPlano(Plano.PRO);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        when(assinaturaRepository.findByUsuario(usuario)).thenReturn(Optional.of(assinatura));
        when(mensagemChatRepository.countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
                eq(1L), eq(AutorMensagem.USUARIO), any(), any())).thenReturn(100L);

        limiteUsoService.verificarLimiteMensagem(usuario);
    }

    @Test
    void assinaturaInadimplenteCaiParaLimitesDoPlanoGratisMesmoSendoPro() {
        Assinatura assinatura = new Assinatura(usuario);
        assinatura.setPlano(Plano.PRO);
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        when(assinaturaRepository.findByUsuario(usuario)).thenReturn(Optional.of(assinatura));
        when(mensagemChatRepository.countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
                eq(1L), eq(AutorMensagem.USUARIO), any(), any())).thenReturn(20L);

        assertThatThrownBy(() -> limiteUsoService.verificarLimiteMensagem(usuario))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("20 mensagens");
    }

    @Test
    void resumoUsoRetornaPlanoEContagensAtuais() {
        semAssinaturaAtiva();
        when(mensagemChatRepository.countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
                eq(1L), eq(AutorMensagem.USUARIO), any(), any())).thenReturn(3L);
        when(relatorioRepository.countByUsuarioAndGeradoEmBetween(eq(usuario), any(), any())).thenReturn(1L);

        LimiteUsoService.ResumoUso resumo = limiteUsoService.resumoUso(usuario);

        assertThat(resumo.plano()).isEqualTo(Plano.GRATIS);
        assertThat(resumo.mensagensNoMes()).isEqualTo(3L);
        assertThat(resumo.relatoriosNoMes()).isEqualTo(1L);
    }

    private void semAssinaturaAtiva() {
        when(assinaturaRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
    }
}
