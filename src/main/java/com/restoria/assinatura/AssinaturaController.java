package com.restoria.assinatura;

import com.restoria.assinatura.dto.AssinaturaResponse;
import com.restoria.assinatura.dto.CheckoutResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * Rotas de assinatura/cobranca:
 * <ul>
 *   <li>{@code GET /api/assinatura} — plano/status atual e uso do mes do usuario autenticado.</li>
 *   <li>{@code POST /api/assinatura/checkout} — cria uma Stripe Checkout Session pro plano PRO.</li>
 *   <li>{@code POST /api/assinatura/webhook} — publico (ver {@code SecurityConfig}), recebe
 *       eventos do Stripe. Autenticidade validada pela assinatura HMAC do payload, nao por JWT.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/assinatura")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    public AssinaturaController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @GetMapping
    public AssinaturaResponse obterAtual() {
        return assinaturaService.obterAtual();
    }

    @PostMapping("/checkout")
    public CheckoutResponse criarCheckout() {
        return assinaturaService.criarSessaoCheckout();
    }

    @PostMapping("/webhook")
    public void webhook(HttpServletRequest request, @RequestHeader("Stripe-Signature") String assinaturaStripe)
            throws IOException {
        String payload;
        try (InputStream corpo = request.getInputStream()) {
            payload = new String(corpo.readAllBytes());
        }
        assinaturaService.processarWebhook(payload, assinaturaStripe);
    }
}
