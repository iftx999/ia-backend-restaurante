package com.restoria.assinatura;

import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimiteUsoServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private UsoMensalRepository usoMensalRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private LimiteUsoService limiteUsoService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        limiteUsoService = new LimiteUsoService(assinaturaRepository, usoMensalRepository, usuarioRepository);
        usuario = new Usuario();
        usuario.setId(1L);
    }

    @Test
    void reservaMensagemQuandoAbaixoDoLimiteDoPlanoGratisEIncrementaOContador() {
        semAssinaturaAtiva();
        UsoMensal uso = usoAtual(19, 0, 0);

        limiteUsoService.reservar(usuario, TipoUso.MENSAGEM);

        assertThat(uso.getMensagens()).isEqualTo(20);
        verify(usuarioRepository).travarPorId(1L);
        verify(usoMensalRepository).save(uso);
    }

    @Test
    void primeiraReservaDoMesCriaOContador() {
        semAssinaturaAtiva();
        when(usoMensalRepository.findByUsuarioIdAndCompetencia(eq(1L), anyString())).thenReturn(Optional.empty());

        limiteUsoService.reservar(usuario, TipoUso.MENSAGEM);

        ArgumentCaptor<UsoMensal> salvo = ArgumentCaptor.forClass(UsoMensal.class);
        verify(usoMensalRepository).save(salvo.capture());
        assertThat(salvo.getValue().getUsuarioId()).isEqualTo(1L);
        assertThat(salvo.getValue().getCompetencia()).isEqualTo(LimiteUsoService.competenciaAtual());
        assertThat(salvo.getValue().getMensagens()).isEqualTo(1);
    }

    @Test
    void bloqueiaMensagemQuandoAtingeLimiteDoPlanoGratis() {
        semAssinaturaAtiva();
        usoAtual(20, 0, 0);

        assertThatThrownBy(() -> limiteUsoService.reservar(usuario, TipoUso.MENSAGEM))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("20 mensagens");
        verify(usoMensalRepository, never()).save(any());
    }

    @Test
    void bloqueiaRelatorioQuandoAtingeLimiteDoPlanoGratis() {
        semAssinaturaAtiva();
        usoAtual(0, 5, 0);

        assertThatThrownBy(() -> limiteUsoService.reservar(usuario, TipoUso.RELATORIO))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("5 relatorios");
    }

    @Test
    void planoProAtivoUsaLimitesMaisAltos() {
        assinatura(Plano.PRO, StatusAssinatura.ATIVA);
        usoAtual(100, 0, 0);

        limiteUsoService.reservar(usuario, TipoUso.MENSAGEM);
    }

    @Test
    void assinaturaInadimplenteCaiParaLimitesDoPlanoGratisMesmoSendoPro() {
        assinatura(Plano.PRO, StatusAssinatura.INADIMPLENTE);
        usoAtual(20, 0, 0);

        assertThatThrownBy(() -> limiteUsoService.reservar(usuario, TipoUso.MENSAGEM))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("20 mensagens");
    }

    @Test
    void bloqueiaImagemNoPlanoGratisSemNemConsultarOContador() {
        semAssinaturaAtiva();

        assertThatThrownBy(() -> limiteUsoService.reservar(usuario, TipoUso.IMAGEM))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("exclusiva do plano PRO");
        verify(usuarioRepository, never()).travarPorId(any());
    }

    @Test
    void permiteImagemNoPlanoProAbaixoDoLimite() {
        assinatura(Plano.PRO, StatusAssinatura.ATIVA);
        usoAtual(0, 0, 19);

        limiteUsoService.reservar(usuario, TipoUso.IMAGEM);
    }

    @Test
    void bloqueiaImagemNoPlanoProQuandoAtingeOLimiteMensal() {
        assinatura(Plano.PRO, StatusAssinatura.ATIVA);
        usoAtual(0, 0, 20);

        assertThatThrownBy(() -> limiteUsoService.reservar(usuario, TipoUso.IMAGEM))
                .isInstanceOf(LimiteUsoExcedidoException.class)
                .hasMessageContaining("20 imagens");
    }

    @Test
    void estornoDevolveUmaUnidadeSemFicarNegativo() {
        UsoMensal uso = usoAtual(1, 0, 0);

        limiteUsoService.estornar(usuario, TipoUso.MENSAGEM);
        limiteUsoService.estornar(usuario, TipoUso.MENSAGEM);

        assertThat(uso.getMensagens()).isZero();
    }

    @Test
    void resumoUsoRetornaPlanoEContagensAtuais() {
        semAssinaturaAtiva();
        usoAtual(3, 1, 0);

        LimiteUsoService.ResumoUso resumo = limiteUsoService.resumoUso(usuario);

        assertThat(resumo.plano()).isEqualTo(Plano.GRATIS);
        assertThat(resumo.mensagensNoMes()).isEqualTo(3L);
        assertThat(resumo.relatoriosNoMes()).isEqualTo(1L);
    }

    @Test
    void resumoUsoSemContadorNoMesRetornaZero() {
        semAssinaturaAtiva();
        when(usoMensalRepository.findByUsuarioIdAndCompetencia(eq(1L), anyString())).thenReturn(Optional.empty());

        LimiteUsoService.ResumoUso resumo = limiteUsoService.resumoUso(usuario);

        assertThat(resumo.mensagensNoMes()).isZero();
        assertThat(resumo.relatoriosNoMes()).isZero();
    }

    private UsoMensal usoAtual(int mensagens, int relatorios, int imagens) {
        UsoMensal uso = new UsoMensal(1L, LimiteUsoService.competenciaAtual());
        uso.somar(TipoUso.MENSAGEM, mensagens);
        uso.somar(TipoUso.RELATORIO, relatorios);
        uso.somar(TipoUso.IMAGEM, imagens);
        when(usoMensalRepository.findByUsuarioIdAndCompetencia(eq(1L), anyString())).thenReturn(Optional.of(uso));
        return uso;
    }

    private void assinatura(Plano plano, StatusAssinatura status) {
        Assinatura assinatura = new Assinatura(usuario);
        assinatura.setPlano(plano);
        assinatura.setStatus(status);
        when(assinaturaRepository.findByUsuario(usuario)).thenReturn(Optional.of(assinatura));
    }

    private void semAssinaturaAtiva() {
        when(assinaturaRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
    }
}
