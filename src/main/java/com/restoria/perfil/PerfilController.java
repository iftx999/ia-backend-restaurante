package com.restoria.perfil;

import com.restoria.perfil.dto.AtualizarLimitesAnaliseRequest;
import com.restoria.perfil.dto.AtualizarPerfilRequest;
import com.restoria.perfil.dto.PerfilResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas protegidas (fora do prefixo {@code /api/auth}, que e publico):
 * <ul>
 *   <li>{@code GET /api/perfil} — dados do usuario autenticado, incluindo
 *       limites de alerta do modulo analitico.</li>
 *   <li>{@code POST /api/perfil/onboarding} — chamada pelo ChatComponent ao
 *       final do onboarding conversacional de primeiro acesso.</li>
 *   <li>{@code PUT /api/perfil/limites-analise} — RF-10, ajusta os limites
 *       usados para marcar anomalias no relatorio analitico.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilResponse buscarPerfilAtual() {
        return perfilService.buscarPerfilAtual();
    }

    @PostMapping("/onboarding")
    public PerfilResponse concluirOnboarding(@Valid @RequestBody AtualizarPerfilRequest request) {
        return perfilService.concluirOnboarding(request);
    }

    @PutMapping("/limites-analise")
    public PerfilResponse atualizarLimitesAnalise(@Valid @RequestBody AtualizarLimitesAnaliseRequest request) {
        return perfilService.atualizarLimitesAnalise(request);
    }
}
