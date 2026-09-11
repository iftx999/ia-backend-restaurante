package com.restoria.analise;

import com.restoria.analise.dto.CompararRelatoriosResponse;
import com.restoria.analise.dto.GerarRelatorioRequest;
import com.restoria.analise.dto.PerguntaRelatorioRequest;
import com.restoria.analise.dto.PerguntaRelatorioResponse;
import com.restoria.analise.dto.RelatorioResponse;
import com.restoria.analise.dto.RelatorioResumoResponse;
import com.restoria.analise.dto.UploadPlanilhaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Modulo analitico (RF-07 a RF-11, Fase 2): upload de planilhas de
 * vendas/estoque e geracao de relatorio via IA.
 */
@RestController
@RequestMapping("/api/analise")
public class AnaliseController {

    private final AnaliseService analiseService;
    private final RelatorioPdfExporter relatorioPdfExporter;
    private final RelatorioExcelExporter relatorioExcelExporter;

    public AnaliseController(
            AnaliseService analiseService,
            RelatorioPdfExporter relatorioPdfExporter,
            RelatorioExcelExporter relatorioExcelExporter) {
        this.analiseService = analiseService;
        this.relatorioPdfExporter = relatorioPdfExporter;
        this.relatorioExcelExporter = relatorioExcelExporter;
    }

    @PostMapping("/upload/vendas")
    public UploadPlanilhaResponse uploadVendas(@RequestParam("arquivo") MultipartFile arquivo) {
        AnaliseService.UploadResultado resultado = analiseService.processarUploadVendas(arquivo);
        return UploadPlanilhaResponse.de(resultado.upload(), resultado.quantidadeLinhas());
    }

    @PostMapping("/upload/estoque")
    public UploadPlanilhaResponse uploadEstoque(@RequestParam("arquivo") MultipartFile arquivo) {
        AnaliseService.UploadResultado resultado = analiseService.processarUploadEstoque(arquivo);
        return UploadPlanilhaResponse.de(resultado.upload(), resultado.quantidadeLinhas());
    }

    @PostMapping("/relatorio")
    public RelatorioResponse gerarRelatorio(@RequestBody GerarRelatorioRequest request) {
        return RelatorioResponse.de(analiseService.gerarRelatorio(request));
    }

    @GetMapping("/relatorio/{id}")
    public RelatorioResponse buscarRelatorio(@PathVariable Long id) {
        return RelatorioResponse.de(analiseService.buscarRelatorio(id));
    }

    @GetMapping("/relatorio")
    public List<RelatorioResumoResponse> listarRelatorios() {
        return analiseService.listarRelatorios().stream().map(RelatorioResumoResponse::de).toList();
    }

    @GetMapping("/relatorio/comparar")
    public CompararRelatoriosResponse compararRelatorios(
            @RequestParam Long idAtual, @RequestParam Long idAnterior) {
        return analiseService.compararRelatorios(idAtual, idAnterior);
    }

    @GetMapping("/relatorio/{id}/pdf")
    public ResponseEntity<byte[]> baixarRelatorioPdf(@PathVariable Long id) {
        Relatorio relatorio = analiseService.buscarRelatorio(id);
        byte[] pdf = relatorioPdfExporter.exportar(relatorio);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/relatorio/{id}/perguntar")
    public PerguntaRelatorioResponse perguntar(@PathVariable Long id, @Valid @RequestBody PerguntaRelatorioRequest request) {
        return new PerguntaRelatorioResponse(analiseService.perguntarSobreRelatorio(id, request.pergunta()));
    }

    @GetMapping("/relatorio/{id}/excel")
    public ResponseEntity<byte[]> baixarRelatorioExcel(@PathVariable Long id) {
        Relatorio relatorio = analiseService.buscarRelatorio(id);
        byte[] xlsx = relatorioExcelExporter.exportar(relatorio);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-" + id + ".xlsx\"")
                .body(xlsx);
    }
}
