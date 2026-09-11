package com.restoria.analise;

import com.restoria.analise.dto.CompararRelatoriosResponse;
import com.restoria.analise.dto.GerarRelatorioRequest;
import com.restoria.assinatura.LimiteUsoService;
import com.restoria.integration.ai.AiConsultantClient;
import com.restoria.integration.ai.AiMensagem;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Orquestra o modo analitico: upload/parsing de planilhas (RF-07/RF-08),
 * calculo de indicadores (RF-09/RF-10) e geracao do relatorio via IA (RF-11).
 */
@Service
public class AnaliseService {

    private final UploadPlanilhaRepository uploadPlanilhaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final ItemEstoqueRepository itemEstoqueRepository;
    private final RelatorioRepository relatorioRepository;
    private final VendasPlanilhaMapeador vendasPlanilhaMapeador;
    private final EstoquePlanilhaMapeador estoquePlanilhaMapeador;
    private final IndicadorCalculator indicadorCalculator;
    private final AiConsultantClient aiConsultantClient;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final RelatorioPersistenciaService relatorioPersistenciaService;
    private final LimiteUsoService limiteUsoService;

    public AnaliseService(
            UploadPlanilhaRepository uploadPlanilhaRepository,
            ItemVendaRepository itemVendaRepository,
            ItemEstoqueRepository itemEstoqueRepository,
            RelatorioRepository relatorioRepository,
            VendasPlanilhaMapeador vendasPlanilhaMapeador,
            EstoquePlanilhaMapeador estoquePlanilhaMapeador,
            IndicadorCalculator indicadorCalculator,
            AiConsultantClient aiConsultantClient,
            UsuarioAutenticadoProvider usuarioAutenticadoProvider,
            RelatorioPersistenciaService relatorioPersistenciaService,
            LimiteUsoService limiteUsoService) {
        this.uploadPlanilhaRepository = uploadPlanilhaRepository;
        this.itemVendaRepository = itemVendaRepository;
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.relatorioRepository = relatorioRepository;
        this.vendasPlanilhaMapeador = vendasPlanilhaMapeador;
        this.estoquePlanilhaMapeador = estoquePlanilhaMapeador;
        this.indicadorCalculator = indicadorCalculator;
        this.aiConsultantClient = aiConsultantClient;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
        this.relatorioPersistenciaService = relatorioPersistenciaService;
        this.limiteUsoService = limiteUsoService;
    }

    @Transactional
    public UploadResultado processarUploadVendas(MultipartFile arquivo) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        UploadPlanilha upload = new UploadPlanilha(usuario, TipoPlanilha.VENDAS, arquivo.getOriginalFilename());

        try {
            List<ItemVenda> itens = vendasPlanilhaMapeador.mapear(arquivo, upload);
            upload.setStatus(StatusUpload.PROCESSADO);
            uploadPlanilhaRepository.save(upload);
            itemVendaRepository.saveAll(itens);
            return new UploadResultado(upload, itens.size());
        } catch (PlanilhaInvalidaException e) {
            upload.setStatus(StatusUpload.ERRO);
            upload.setMensagemErro(e.getMessage());
            uploadPlanilhaRepository.save(upload);
            throw e;
        }
    }

    @Transactional
    public UploadResultado processarUploadEstoque(MultipartFile arquivo) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        UploadPlanilha upload = new UploadPlanilha(usuario, TipoPlanilha.ESTOQUE, arquivo.getOriginalFilename());

        try {
            List<ItemEstoque> itens = estoquePlanilhaMapeador.mapear(arquivo, upload);
            upload.setStatus(StatusUpload.PROCESSADO);
            uploadPlanilhaRepository.save(upload);
            itemEstoqueRepository.saveAll(itens);
            return new UploadResultado(upload, itens.size());
        } catch (PlanilhaInvalidaException e) {
            upload.setStatus(StatusUpload.ERRO);
            upload.setMensagemErro(e.getMessage());
            uploadPlanilhaRepository.save(upload);
            throw e;
        }
    }

    /**
     * Nao e {@code @Transactional} de ponta a ponta pelo mesmo motivo de
     * {@code ChatService.responderStream}: a chamada a IA pode levar varios
     * segundos e nao deve prender uma conexao do pool durante esse tempo.
     */
    public Relatorio gerarRelatorio(GerarRelatorioRequest request) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        limiteUsoService.verificarLimiteRelatorio(usuario);

        if (request.uploadVendasId() == null && request.uploadEstoqueId() == null) {
            throw new PlanilhaInvalidaException("Informe ao menos um upload de vendas ou de estoque");
        }

        UploadPlanilha uploadVendas = buscarUpload(request.uploadVendasId(), usuario);
        UploadPlanilha uploadEstoque = buscarUpload(request.uploadEstoqueId(), usuario);

        List<ItemVenda> vendas = uploadVendas == null ? List.of() : itemVendaRepository.findByUpload(uploadVendas);
        List<ItemEstoque> estoque = uploadEstoque == null ? List.of() : itemEstoqueRepository.findByUpload(uploadEstoque);

        ResultadoAnalise resultado = indicadorCalculator.calcular(
                vendas, estoque, usuario.getMargemMinimaEsperada(), usuario.getPercentualPerdaAlerta());
        String textoIa = gerarTextoIa(resultado);

        return relatorioPersistenciaService.persistir(usuario, uploadVendas, uploadEstoque, resultado, textoIa);
    }

    private UploadPlanilha buscarUpload(Long id, Usuario usuario) {
        if (id == null) {
            return null;
        }
        return uploadPlanilhaRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new UploadNaoEncontradoException(id));
    }

    private String gerarTextoIa(ResultadoAnalise resultado) {
        String resumo = AnaliseResumoFormatter.formatar(resultado);
        return aiConsultantClient.enviarMensagem(AnaliseSystemPrompt.TEXTO, List.of(AiMensagem.doUsuario(resumo)));
    }

    public Relatorio buscarRelatorio(Long id) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        return relatorioRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new RelatorioNaoEncontradoException(id));
    }

    /** RF-15: historico de relatorios do usuario, mais recente primeiro. */
    public List<Relatorio> listarRelatorios() {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        return relatorioRepository.findByUsuarioOrderByGeradoEmDesc(usuario);
    }

    /**
     * RF-15: compara dois relatorios do usuario (ex: CMV mes a mes).
     * {@code buscarRelatorio} ja garante que ambos pertencem ao usuario
     * autenticado.
     */
    public CompararRelatoriosResponse compararRelatorios(Long idAtual, Long idAnterior) {
        Relatorio atual = buscarRelatorio(idAtual);
        Relatorio anterior = buscarRelatorio(idAnterior);
        return RelatorioComparador.comparar(atual, anterior);
    }

    /**
     * RF-12: usuario pergunta sobre um relatorio ja gerado (ex: "por que
     * esse prato esta com margem baixa?"). Pergunta pontual, sem historico
     * persistido — a IA recebe o texto do relatorio como contexto (papel
     * "assistant") seguido da pergunta do usuario.
     */
    public String perguntarSobreRelatorio(Long id, String pergunta) {
        Relatorio relatorio = buscarRelatorio(id);
        List<AiMensagem> mensagens = List.of(
                AiMensagem.daIa(relatorio.getConteudoTextoIA()),
                AiMensagem.doUsuario(pergunta));
        return aiConsultantClient.enviarMensagem(AnaliseSystemPrompt.TEXTO, mensagens);
    }

    public record UploadResultado(UploadPlanilha upload, int quantidadeLinhas) {
    }
}
