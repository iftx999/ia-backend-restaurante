package com.restoria.integration.billing;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * O SDK do Stripe usa uma chave de API global estatica ({@link Stripe#apiKey}),
 * diferente do padrao de {@code RestClient} usado em {@code integration.ai}
 * pra Anthropic/Voyage — e assim que o SDK oficial funciona.
 */
@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    private final StripeProperties properties;

    public StripeConfig(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void configurarChaveApi() {
        Stripe.apiKey = properties.apiKey();
    }
}
