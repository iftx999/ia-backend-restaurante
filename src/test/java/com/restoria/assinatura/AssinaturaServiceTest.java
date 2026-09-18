package com.restoria.assinatura;

import com.restoria.integration.billing.StripeProperties;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A verificacao de assinatura do webhook (HMAC) e 100% local (nao chama a API
 * do Stripe), entao da pra testar {@code processarWebhook} de ponta a ponta
 * assinando os payloads manualmente com o mesmo algoritmo do SDK (ver
 * {@code com.stripe.net.Webhook.Signature.computeSignature}).
 */
@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    private static final String WEBHOOK_SECRET = "whsec_teste_1234567890";
    private static final String API_VERSION_COMPATIVEL = "2025-07-30.basil";

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    @Mock
    private LimiteUsoService limiteUsoService;

    private AssinaturaService assinaturaService;

    @BeforeEach
    void setUp() {
        StripeProperties stripeProperties = new StripeProperties(
                "sk_test_fake", WEBHOOK_SECRET, "price_fake",
                "http://localhost:4200/planos?checkout=sucesso", "http://localhost:4200/planos?checkout=cancelado");
        assinaturaService = new AssinaturaService(
                assinaturaRepository, usuarioAutenticadoProvider, limiteUsoService, stripeProperties);
    }

    @Test
    void lancaExcecaoQuandoAssinaturaDoWebhookEInvalida() {
        String payload = "{}";
        String headerComAssinaturaErrada = assinar(payload, "outro-secret");

        assertThatThrownBy(() -> assinaturaService.processarWebhook(payload, headerComAssinaturaErrada))
                .isInstanceOf(WebhookInvalidoException.class);

        verifyNoInteractions(assinaturaRepository);
    }

    @Test
    void checkoutConcluidoAtivaPlanoProEAssociaSubscriptionId() {
        String payload = """
                {
                  "id": "evt_1",
                  "object": "event",
                  "api_version": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_1",
                      "object": "checkout.session",
                      "customer": "cus_1",
                      "subscription": "sub_1"
                    }
                  }
                }
                """.formatted(API_VERSION_COMPATIVEL);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Assinatura assinatura = new Assinatura(usuario);
        assinatura.setStripeCustomerId("cus_1");
        when(assinaturaRepository.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(assinatura));

        assinaturaService.processarWebhook(payload, assinar(payload, WEBHOOK_SECRET));

        assertThat(assinatura.getPlano()).isEqualTo(Plano.PRO);
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getStripeSubscriptionId()).isEqualTo("sub_1");
        verify(assinaturaRepository).save(assinatura);
    }

    @Test
    void checkoutConcluidoSemAssinaturaCorrespondenteLancaExcecao() {
        String payload = """
                {
                  "id": "evt_1",
                  "object": "event",
                  "api_version": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_1",
                      "object": "checkout.session",
                      "customer": "cus_desconhecido",
                      "subscription": "sub_1"
                    }
                  }
                }
                """.formatted(API_VERSION_COMPATIVEL);
        when(assinaturaRepository.findByStripeCustomerId("cus_desconhecido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assinaturaService.processarWebhook(payload, assinar(payload, WEBHOOK_SECRET)))
                .isInstanceOf(AssinaturaNaoEncontradaException.class);
    }

    @Test
    void atualizacaoDeAssinaturaSincronizaStatusEDataDeRenovacao() {
        String payload = """
                {
                  "id": "evt_2",
                  "object": "event",
                  "api_version": "%s",
                  "type": "customer.subscription.updated",
                  "data": {
                    "object": {
                      "id": "sub_1",
                      "object": "subscription",
                      "status": "past_due",
                      "items": {
                        "object": "list",
                        "data": [
                          {
                            "id": "si_1",
                            "object": "subscription_item",
                            "current_period_end": 1780000000
                          }
                        ],
                        "has_more": false,
                        "url": "/v1/subscription_items"
                      }
                    }
                  }
                }
                """.formatted(API_VERSION_COMPATIVEL);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Assinatura assinatura = new Assinatura(usuario);
        assinatura.setPlano(Plano.PRO);
        assinatura.setStripeSubscriptionId("sub_1");
        when(assinaturaRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(assinatura));

        assinaturaService.processarWebhook(payload, assinar(payload, WEBHOOK_SECRET));

        // past_due mapeia pra INADIMPLENTE (nao ATIVA nem CANCELADA) - ver AssinaturaService.mapearStatus
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(assinatura.getPlano()).isEqualTo(Plano.PRO);
        assertThat(assinatura.getRenovaEm()).isNotNull();
        verify(assinaturaRepository).save(assinatura);
    }

    @Test
    void cancelamentoDeAssinaturaVoltaParaPlanoGratis() {
        String payload = """
                {
                  "id": "evt_3",
                  "object": "event",
                  "api_version": "%s",
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "sub_1",
                      "object": "subscription",
                      "status": "canceled",
                      "items": {
                        "object": "list",
                        "data": [],
                        "has_more": false,
                        "url": "/v1/subscription_items"
                      }
                    }
                  }
                }
                """.formatted(API_VERSION_COMPATIVEL);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Assinatura assinatura = new Assinatura(usuario);
        assinatura.setPlano(Plano.PRO);
        assinatura.setStripeSubscriptionId("sub_1");
        when(assinaturaRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(assinatura));

        assinaturaService.processarWebhook(payload, assinar(payload, WEBHOOK_SECRET));

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.getPlano()).isEqualTo(Plano.GRATIS);
    }

    @Test
    void eventoDeTipoSemTratamentoEspecificoENaoInteraeComRepositorio() {
        String payload = """
                {
                  "id": "evt_4",
                  "object": "event",
                  "api_version": "%s",
                  "type": "invoice.paid",
                  "data": {
                    "object": {
                      "id": "in_1",
                      "object": "invoice"
                    }
                  }
                }
                """.formatted(API_VERSION_COMPATIVEL);

        assinaturaService.processarWebhook(payload, assinar(payload, WEBHOOK_SECRET));

        verifyNoInteractions(assinaturaRepository);
    }

    @Test
    void eventoComVersaoDeApiIncompativelEIgnoradoSemErro() {
        String payload = """
                {
                  "id": "evt_5",
                  "object": "event",
                  "api_version": "2019-01-01",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_1",
                      "object": "checkout.session",
                      "customer": "cus_1",
                      "subscription": "sub_1"
                    }
                  }
                }
                """;

        assinaturaService.processarWebhook(payload, assinar(payload, WEBHOOK_SECRET));

        verifyNoInteractions(assinaturaRepository);
    }

    @Test
    void obterAtualRetornaPlanoStatusEUsoDoUsuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioAutenticadoProvider.obterAtual()).thenReturn(usuario);
        Assinatura assinatura = new Assinatura(usuario);
        when(assinaturaRepository.findByUsuario(usuario)).thenReturn(Optional.of(assinatura));
        when(limiteUsoService.resumoUso(usuario))
                .thenReturn(new LimiteUsoService.ResumoUso(Plano.GRATIS, 3, 1));

        var resposta = assinaturaService.obterAtual();

        assertThat(resposta.plano()).isEqualTo("GRATIS");
        assertThat(resposta.status()).isEqualTo("ATIVA");
        assertThat(resposta.mensagensUsadasNoMes()).isEqualTo(3);
        assertThat(resposta.relatoriosUsadosNoMes()).isEqualTo(1);
    }

    /** Reimplementa a assinatura HMAC/SHA-256 do Stripe (com.stripe.net.Webhook.Signature). */
    private String assinar(String payload, String secret) {
        long timestamp = System.currentTimeMillis() / 1000L;
        String payloadAssinado = timestamp + "." + payload;
        try {
            Mac hasher = Mac.getInstance("HmacSHA256");
            hasher.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = hasher.doFinal(payloadAssinado.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "t=" + timestamp + ",v1=" + hex;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
