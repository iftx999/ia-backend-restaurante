package com.restoria.integration.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mapeia o bloco `restoria.stripe` do application.yml. As chaves nunca tem
 * default hardcoded: vem so de STRIPE_API_KEY / STRIPE_WEBHOOK_SECRET /
 * STRIPE_PRICE_ID_PRO (mesmo padrao de {@code AiProperties} pra Anthropic).
 */
@ConfigurationProperties(prefix = "restoria.stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String priceIdPro,
        String successUrl,
        String cancelUrl
) {
}
