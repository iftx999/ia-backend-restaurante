package com.restoria.conhecimento;

import com.restoria.conhecimento.dto.IngestaoDocumentoRequest;
import com.restoria.conhecimento.dto.IngestaoDocumentoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestao de documentos na base de conhecimento (RF-19). Sem autenticacao
 * por enquanto, igual ao restante do projeto nesta fase.
 */
@RestController
@RequestMapping("/api/conhecimento")
public class ConhecimentoController {

    private final ConhecimentoService conhecimentoService;
    private final TrechoConhecimentoRepository trechoConhecimentoRepository;

    public ConhecimentoController(
            ConhecimentoService conhecimentoService,
            TrechoConhecimentoRepository trechoConhecimentoRepository) {
        this.conhecimentoService = conhecimentoService;
        this.trechoConhecimentoRepository = trechoConhecimentoRepository;
    }

    @PostMapping("/documentos")
    @ResponseStatus(HttpStatus.CREATED)
    public IngestaoDocumentoResponse ingerir(@Valid @RequestBody IngestaoDocumentoRequest request) {
        DocumentoConhecimento documento = conhecimentoService.ingerir(
                request.titulo(), request.conteudo(), request.fonte());

        long quantidadeTrechos = trechoConhecimentoRepository.countByDocumentoId(documento.getId());

        return new IngestaoDocumentoResponse(documento.getId(), documento.getTitulo(), (int) quantidadeTrechos);
    }
}
