package com.restoria.integration.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControleChamadaIaTest {

    private final ControleChamadaIa controle = new ControleChamadaIa("provedor-teste", 1, Duration.ofMillis(100), 3, 0);

    @Test
    void repeteChamadaEmRespostaTransitoriaAteDarCerto() {
        AtomicInteger tentativas = new AtomicInteger();

        String resultado = controle.executar(() -> {
            if (tentativas.incrementAndGet() < 3) {
                throw erroHttp(529);
            }
            return "ok";
        });

        assertThat(resultado).isEqualTo("ok");
        assertThat(tentativas.get()).isEqualTo(3);
    }

    @Test
    void desisteDepoisDoMaximoDeTentativas() {
        AtomicInteger tentativas = new AtomicInteger();

        assertThatThrownBy(() -> controle.executar(() -> {
            tentativas.incrementAndGet();
            throw erroHttp(429);
        })).isInstanceOf(RestClientResponseException.class);

        assertThat(tentativas.get()).isEqualTo(3);
    }

    @Test
    void naoRepeteErroDoCliente() {
        AtomicInteger tentativas = new AtomicInteger();

        assertThatThrownBy(() -> controle.executar(() -> {
            tentativas.incrementAndGet();
            throw erroHttp(400);
        })).isInstanceOf(RestClientResponseException.class);

        assertThat(tentativas.get()).isEqualTo(1);
    }

    @Test
    void recusaQuandoTodasAsVagasEstaoOcupadasAlemDaEsperaMaxima() throws Exception {
        CountDownLatch vagaOcupada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);

        Thread ocupante = Thread.ofVirtual().start(() -> controle.executar(() -> {
            vagaOcupada.countDown();
            try {
                liberar.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        vagaOcupada.await();

        assertThatThrownBy(() -> controle.executar(() -> "nao deveria rodar"))
                .isInstanceOf(IaSobrecarregadaException.class);

        liberar.countDown();
        ocupante.join();
        assertThat(controle.vagasDisponiveis()).isEqualTo(1);
    }

    private RestClientResponseException erroHttp(int status) {
        return new RestClientResponseException("HTTP " + status, HttpStatusCode.valueOf(status), "erro",
                new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }
}
