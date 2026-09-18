package com.restoria.imagem;

import com.restoria.assinatura.LimiteUsoExcedidoException;
import com.restoria.assinatura.LimiteUsoService;
import com.restoria.chat.ImagemInvalidaException;
import com.restoria.imagem.dto.EditarImagemRequest;
import com.restoria.imagem.dto.GerarImagemRequest;
import com.restoria.integration.ai.ImagemGerada;
import com.restoria.integration.ai.ImagemIaClient;
import com.restoria.integration.ai.TamanhoImagem;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagemPratoServiceTest {

    @Mock
    private ImagemIaClient imagemIaClient;

    @Mock
    private ImagemStorageService storageService;

    @Mock
    private ImagemPratoRepository imagemPratoRepository;

    @Mock
    private LimiteUsoService limiteUsoService;

    @Mock
    private UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    private ImagemPratoService imagemPratoService;
    private final Usuario usuario = new Usuario();

    @BeforeEach
    void setUp() {
        imagemPratoService = new ImagemPratoService(
                imagemIaClient, storageService, imagemPratoRepository, limiteUsoService, usuarioAutenticadoProvider);
        usuario.setId(1L);
        when(usuarioAutenticadoProvider.obterAtual()).thenReturn(usuario);
    }

    @Test
    void gerarChamaOClientSalvaOArquivoEPersisteAEntidade() {
        when(imagemIaClient.gerar("Feijoada em um prato branco", TamanhoImagem.QUADRADO))
                .thenReturn(new ImagemGerada(new byte[]{1, 2, 3}, "image/png"));
        when(storageService.salvar(eq(1L), any(byte[].class), eq("image/png"))).thenReturn("1/abc.png");
        when(imagemPratoRepository.save(any(ImagemPrato.class))).thenAnswer(inv -> inv.getArgument(0));

        ImagemPrato resultado = imagemPratoService.gerar(new GerarImagemRequest("Feijoada em um prato branco", null));

        assertThat(resultado.getTipoOperacao()).isEqualTo(TipoOperacaoImagem.GERACAO);
        assertThat(resultado.getCaminhoArquivo()).isEqualTo("1/abc.png");
        assertThat(resultado.getImagemOriginalCaminho()).isNull();
        verify(limiteUsoService).verificarLimiteImagem(usuario);
    }

    @Test
    void gerarPropagaLimiteUsoExcedidoSemChamarOClientDeImagem() {
        doThrow(new LimiteUsoExcedidoException("limite atingido")).when(limiteUsoService).verificarLimiteImagem(usuario);

        assertThatThrownBy(() -> imagemPratoService.gerar(new GerarImagemRequest("prompt qualquer", null)))
                .isInstanceOf(LimiteUsoExcedidoException.class);

        verify(imagemIaClient, never()).gerar(any(), any());
    }

    @Test
    void gerarNormalizaTamanhoPaisagem() {
        when(imagemIaClient.gerar(any(), eq(TamanhoImagem.PAISAGEM)))
                .thenReturn(new ImagemGerada(new byte[]{1}, "image/png"));
        when(storageService.salvar(anyLong(), any(), any())).thenReturn("1/abc.png");
        when(imagemPratoRepository.save(any(ImagemPrato.class))).thenAnswer(inv -> inv.getArgument(0));

        imagemPratoService.gerar(new GerarImagemRequest("prato num prato retangular", "paisagem"));

        verify(imagemIaClient).gerar(any(), eq(TamanhoImagem.PAISAGEM));
    }

    @Test
    void editarDecodificaImagemOriginalESalvaAmbosOsArquivos() {
        String base64 = Base64.getEncoder().encodeToString(new byte[]{9, 9, 9});
        when(imagemIaClient.editar(eq("troca o fundo"), any(byte[].class), eq("image/jpeg"), eq(TamanhoImagem.QUADRADO)))
                .thenReturn(new ImagemGerada(new byte[]{7, 7}, "image/png"));
        when(storageService.salvar(eq(1L), any(byte[].class), eq("image/jpeg"))).thenReturn("1/original.jpg");
        when(storageService.salvar(eq(1L), any(byte[].class), eq("image/png"))).thenReturn("1/editada.png");
        when(imagemPratoRepository.save(any(ImagemPrato.class))).thenAnswer(inv -> inv.getArgument(0));

        ImagemPrato resultado = imagemPratoService.editar(
                new EditarImagemRequest("troca o fundo", base64, "image/jpeg", null));

        assertThat(resultado.getTipoOperacao()).isEqualTo(TipoOperacaoImagem.EDICAO);
        assertThat(resultado.getImagemOriginalCaminho()).isEqualTo("1/original.jpg");
        assertThat(resultado.getCaminhoArquivo()).isEqualTo("1/editada.png");
    }

    @Test
    void editarComBase64InvalidoLancaImagemInvalida() {
        assertThatThrownBy(() -> imagemPratoService.editar(
                new EditarImagemRequest("prompt", "isso-nao-e-base64-valido!!!", "image/png", null)))
                .isInstanceOf(ImagemInvalidaException.class);

        verify(imagemIaClient, never()).editar(any(), any(), any(), any());
    }

    @Test
    void buscarRetornaImagemDoUsuarioAutenticado() {
        ImagemPrato imagem = new ImagemPrato(usuario, "prompt", TipoOperacaoImagem.GERACAO, "1/abc.png");
        when(imagemPratoRepository.findByIdAndUsuario(42L, usuario)).thenReturn(Optional.of(imagem));

        assertThat(imagemPratoService.buscar(42L)).isSameAs(imagem);
    }

    @Test
    void buscarLancaExcecaoQuandoNaoEncontradaOuDeOutroUsuario() {
        when(imagemPratoRepository.findByIdAndUsuario(42L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imagemPratoService.buscar(42L))
                .isInstanceOf(ImagemPratoNaoEncontradaException.class);
    }
}
