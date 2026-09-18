package com.restoria.imagem;

import com.restoria.imagem.dto.EditarImagemRequest;
import com.restoria.imagem.dto.GerarImagemRequest;
import com.restoria.imagem.dto.ImagemPratoResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Geracao/edicao de imagem de prato via IA (plano PRO — ver
 * docs/06-geracao-imagem-ia.md). Autenticacao/autorizacao por usuario ja
 * cobertas pelo {@code JwtAuthenticationFilter} (rota nao publica) e por
 * {@code ImagemPratoService} (toda consulta filtra pelo usuario autenticado).
 */
@RestController
@RequestMapping("/api/imagens")
public class ImagemPratoController {

    private final ImagemPratoService imagemPratoService;

    public ImagemPratoController(ImagemPratoService imagemPratoService) {
        this.imagemPratoService = imagemPratoService;
    }

    @PostMapping("/gerar")
    public ImagemPratoResponse gerar(@Valid @RequestBody GerarImagemRequest request) {
        return ImagemPratoResponse.de(imagemPratoService.gerar(request));
    }

    @PostMapping("/editar")
    public ImagemPratoResponse editar(@Valid @RequestBody EditarImagemRequest request) {
        return ImagemPratoResponse.de(imagemPratoService.editar(request));
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id) {
        ImagemPrato imagem = imagemPratoService.buscar(id);
        byte[] dados = imagemPratoService.lerArquivo(imagem);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(dados);
    }
}
