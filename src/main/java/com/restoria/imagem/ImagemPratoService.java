package com.restoria.imagem;

import com.restoria.assinatura.LimiteUsoService;
import com.restoria.chat.ImagemInvalidaException;
import com.restoria.imagem.dto.EditarImagemRequest;
import com.restoria.imagem.dto.GerarImagemRequest;
import com.restoria.integration.ai.ImagemGerada;
import com.restoria.integration.ai.ImagemIaClient;
import com.restoria.integration.ai.TamanhoImagem;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Locale;

/**
 * Orquestra geracao/edicao de imagem de prato (plano PRO — ver
 * docs/06-geracao-imagem-ia.md). Nao e {@code @Transactional} de ponta a
 * ponta: a chamada a API de imagens da OpenAI pode levar 10-30s, e prender
 * uma conexao do pool do HikariCP durante esse tempo esgotaria o pool (mesmo
 * motivo documentado em {@code ChatService.responderStream}/
 * {@code AnaliseService.gerarRelatorio}).
 */
@Service
public class ImagemPratoService {

    private final ImagemIaClient imagemIaClient;
    private final ImagemStorageService storageService;
    private final ImagemPratoRepository imagemPratoRepository;
    private final LimiteUsoService limiteUsoService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public ImagemPratoService(
            ImagemIaClient imagemIaClient,
            ImagemStorageService storageService,
            ImagemPratoRepository imagemPratoRepository,
            LimiteUsoService limiteUsoService,
            UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.imagemIaClient = imagemIaClient;
        this.storageService = storageService;
        this.imagemPratoRepository = imagemPratoRepository;
        this.limiteUsoService = limiteUsoService;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    public ImagemPrato gerar(GerarImagemRequest request) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        limiteUsoService.verificarLimiteImagem(usuario);

        TamanhoImagem tamanho = normalizarTamanho(request.tamanho());
        ImagemGerada imagem = imagemIaClient.gerar(request.prompt(), tamanho);
        String caminho = storageService.salvar(usuario.getId(), imagem.dados(), imagem.mediaType());

        ImagemPrato entidade = new ImagemPrato(usuario, request.prompt(), TipoOperacaoImagem.GERACAO, caminho);
        return imagemPratoRepository.save(entidade);
    }

    public ImagemPrato editar(EditarImagemRequest request) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        limiteUsoService.verificarLimiteImagem(usuario);

        byte[] imagemOriginal = decodificar(request.imagemBase64());
        TamanhoImagem tamanho = normalizarTamanho(request.tamanho());

        ImagemGerada imagem = imagemIaClient.editar(request.prompt(), imagemOriginal, request.imagemMediaType(), tamanho);

        String caminhoOriginal = storageService.salvar(usuario.getId(), imagemOriginal, request.imagemMediaType());
        String caminhoResultado = storageService.salvar(usuario.getId(), imagem.dados(), imagem.mediaType());

        ImagemPrato entidade = new ImagemPrato(usuario, request.prompt(), TipoOperacaoImagem.EDICAO, caminhoResultado);
        entidade.setImagemOriginalCaminho(caminhoOriginal);
        return imagemPratoRepository.save(entidade);
    }

    public ImagemPrato buscar(Long id) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        return imagemPratoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ImagemPratoNaoEncontradaException(id));
    }

    public byte[] lerArquivo(ImagemPrato imagem) {
        return storageService.ler(imagem.getCaminhoArquivo());
    }

    private byte[] decodificar(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new ImagemInvalidaException("Imagem original invalida (base64 malformado)");
        }
    }

    private TamanhoImagem normalizarTamanho(String valor) {
        if (valor == null || valor.isBlank()) {
            return TamanhoImagem.QUADRADO;
        }
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "paisagem" -> TamanhoImagem.PAISAGEM;
            case "retrato" -> TamanhoImagem.RETRATO;
            default -> TamanhoImagem.QUADRADO;
        };
    }
}
