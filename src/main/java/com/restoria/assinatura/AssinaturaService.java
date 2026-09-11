package com.restoria.assinatura;

import com.restoria.assinatura.dto.AssinaturaResponse;
import com.restoria.assinatura.dto.CheckoutResponse;
import com.restoria.integration.billing.StripeProperties;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Checkout + webhook do Stripe (assinatura do plano PRO). Cálculo de limite
 * de uso fica em {@link LimiteUsoService} — esta classe só cuida do ciclo de
 * vida da assinatura em si (criar sessão de checkout, sincronizar status a
 * partir dos eventos do Stripe).
 */
@Service
public class AssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaService.class);

    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final LimiteUsoService limiteUsoService;
    private final StripeProperties stripeProperties;

    public AssinaturaService(
            AssinaturaRepository assinaturaRepository,
            UsuarioAutenticadoProvider usuarioAutenticadoProvider,
            LimiteUsoService limiteUsoService,
            StripeProperties stripeProperties) {
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
        this.limiteUsoService = limiteUsoService;
        this.stripeProperties = stripeProperties;
    }

    @Transactional(readOnly = true)
    public AssinaturaResponse obterAtual() {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        Assinatura assinatura = buscarOuCriar(usuario);
        LimiteUsoService.ResumoUso uso = limiteUsoService.resumoUso(usuario);

        return new AssinaturaResponse(
                assinatura.getPlano().name(),
                assinatura.getStatus().name(),
                uso.mensagensNoMes(),
                uso.plano().getMensagensPorMes(),
                uso.relatoriosNoMes(),
                uso.plano().getRelatoriosPorMes());
    }

    /** Cria (ou reaproveita) o Customer do Stripe do usuario e abre uma Checkout Session pro plano PRO. */
    @Transactional
    public CheckoutResponse criarSessaoCheckout() {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        Assinatura assinatura = buscarOuCriar(usuario);

        try {
            String customerId = assinatura.getStripeCustomerId();
            if (customerId == null) {
                Customer customer = Customer.create(CustomerCreateParams.builder()
                        .setEmail(usuario.getEmail())
                        .setName(usuario.getNome())
                        .putMetadata("usuarioId", String.valueOf(usuario.getId()))
                        .build());
                customerId = customer.getId();
                assinatura.setStripeCustomerId(customerId);
                assinaturaRepository.save(assinatura);
            }

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setClientReferenceId(String.valueOf(usuario.getId()))
                    .setSuccessUrl(stripeProperties.successUrl())
                    .setCancelUrl(stripeProperties.cancelUrl())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(stripeProperties.priceIdPro())
                            .setQuantity(1L)
                            .build())
                    .build();

            Session session = Session.create(params);
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            throw new AssinaturaIndisponivelException(e);
        }
    }

    /**
     * Processa um evento de webhook do Stripe. A validade do payload e verificada
     * pela assinatura HMAC contra {@code restoria.stripe.webhook-secret} — nunca
     * confiar em dados de billing sem essa verificacao (qualquer um poderia
     * chamar o endpoint publico direto).
     */
    @Transactional
    public void processarWebhook(String payload, String assinaturaStripeHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, assinaturaStripeHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new WebhookInvalidoException(e);
        }

        Optional<StripeObject> objeto = event.getDataObjectDeserializer().getObject();
        if (objeto.isEmpty()) {
            log.warn("Webhook Stripe {} sem payload deserializavel, ignorando", event.getType());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> tratarCheckoutConcluido((Session) objeto.get());
            case "customer.subscription.updated" -> tratarAtualizacaoAssinatura((Subscription) objeto.get());
            case "customer.subscription.deleted" -> tratarCancelamento((Subscription) objeto.get());
            default -> log.debug("Evento Stripe {} ignorado (sem tratamento especifico)", event.getType());
        }
    }

    private void tratarCheckoutConcluido(Session session) {
        Assinatura assinatura = assinaturaRepository.findByStripeCustomerId(session.getCustomer())
                .orElseThrow(() -> new AssinaturaNaoEncontradaException(session.getCustomer()));

        assinatura.setPlano(Plano.PRO);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setStripeSubscriptionId(session.getSubscription());
        assinaturaRepository.save(assinatura);
    }

    private void tratarAtualizacaoAssinatura(Subscription subscription) {
        assinaturaRepository.findByStripeSubscriptionId(subscription.getId()).ifPresentOrElse(assinatura -> {
            assinatura.setStatus(mapearStatus(subscription.getStatus()));
            // A partir da API 2025 do Stripe, o periodo de cobranca vive no item da
            // assinatura (subscription.items.data[0]), nao mais em Subscription direto.
            Long fimPeriodo = subscription.getItems().getData().stream()
                    .findFirst()
                    .map(SubscriptionItem::getCurrentPeriodEnd)
                    .orElse(null);
            if (fimPeriodo != null) {
                assinatura.setRenovaEm(Instant.ofEpochSecond(fimPeriodo).atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            assinaturaRepository.save(assinatura);
        }, () -> log.warn("customer.subscription.updated para subscription {} sem Assinatura correspondente", subscription.getId()));
    }

    private void tratarCancelamento(Subscription subscription) {
        assinaturaRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(assinatura -> {
            assinatura.setStatus(StatusAssinatura.CANCELADA);
            assinatura.setPlano(Plano.GRATIS);
            assinaturaRepository.save(assinatura);
        });
    }

    private StatusAssinatura mapearStatus(String statusStripe) {
        return switch (statusStripe) {
            case "active", "trialing" -> StatusAssinatura.ATIVA;
            case "canceled", "unpaid" -> StatusAssinatura.CANCELADA;
            default -> StatusAssinatura.INADIMPLENTE; // past_due, incomplete, incomplete_expired, paused
        };
    }

    private Assinatura buscarOuCriar(Usuario usuario) {
        return assinaturaRepository.findByUsuario(usuario)
                .orElseGet(() -> assinaturaRepository.save(new Assinatura(usuario)));
    }
}
