package com.restoria.integration.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Protecao comum das chamadas a um provedor de IA:
 * <ul>
 *   <li><b>Bulkhead</b>: no maximo {@code maxConcorrencia} chamadas simultaneas
 *       por instancia. Quem passa disso espera ate {@code esperaMaxima} por uma
 *       vaga e, depois, recebe {@link IaSobrecarregadaException} (HTTP 503).</li>
 *   <li><b>Retry com backoff exponencial + jitter</b> para respostas
 *       transitorias do provedor (429, 500, 502, 503, 529), respeitando o
 *       header {@code retry-after} quando vier.</li>
 * </ul>
 * Com virtual threads, esperar pela vaga ou dormir no backoff nao prende
 * thread de plataforma.
 */
public class ControleChamadaIa {

    private static final Logger log = LoggerFactory.getLogger(ControleChamadaIa.class);

    private static final Set<Integer> STATUS_TRANSITORIOS = Set.of(429, 500, 502, 503, 529);
    private static final long BACKOFF_MAXIMO_MS = 10_000;

    private final String provedor;
    private final Semaphore vagas;
    private final Duration esperaMaxima;
    private final int maxTentativas;
    private final long backoffInicialMs;

    public ControleChamadaIa(String provedor, int maxConcorrencia, Duration esperaMaxima,
                             int maxTentativas, long backoffInicialMs) {
        this.provedor = provedor;
        this.vagas = new Semaphore(maxConcorrencia, true);
        this.esperaMaxima = esperaMaxima;
        this.maxTentativas = Math.max(1, maxTentativas);
        this.backoffInicialMs = backoffInicialMs;
    }

    public static ControleChamadaIa padrao(String provedor, int maxConcorrencia) {
        return new ControleChamadaIa(provedor, maxConcorrencia, Duration.ofSeconds(20), 3, 1_000);
    }

    public <T> T executar(Supplier<T> chamada) {
        adquirirVaga();
        try {
            return comRetentativa(chamada);
        } finally {
            vagas.release();
        }
    }

    public void executar(Runnable chamada) {
        executar(() -> {
            chamada.run();
            return null;
        });
    }

    /**
     * Para chamadas em streaming via {@code RestClient.exchange}, que nao
     * lanca excecao em status de erro: converte 4xx/5xx em
     * {@link RestClientResponseException} para passar pelo retry.
     */
    public static void lancarSeErro(ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (status.isError()) {
            byte[] corpo = response.getBody().readAllBytes();
            throw new RestClientResponseException("HTTP " + status.value() + " ao abrir o stream",
                    status, response.getStatusText(), response.getHeaders(), corpo, StandardCharsets.UTF_8);
        }
    }

    /** Vagas livres agora — exposto para metricas/testes. */
    public int vagasDisponiveis() {
        return vagas.availablePermits();
    }

    private void adquirirVaga() {
        try {
            if (!vagas.tryAcquire(esperaMaxima.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("Bulkhead de {} saturado: requisicao recusada apos {}s de espera", provedor, esperaMaxima.toSeconds());
                throw new IaSobrecarregadaException(provedor);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaSobrecarregadaException(provedor);
        }
    }

    private <T> T comRetentativa(Supplier<T> chamada) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return chamada.get();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (!STATUS_TRANSITORIOS.contains(status) || tentativa >= maxTentativas) {
                    throw e;
                }
                long espera = calcularEspera(tentativa, e.getResponseHeaders());
                log.warn("{} respondeu HTTP {} (tentativa {}/{}); nova tentativa em {}ms",
                        provedor, status, tentativa, maxTentativas, espera);
                dormir(espera);
            }
        }
    }

    private long calcularEspera(int tentativa, HttpHeaders headers) {
        if (headers != null) {
            String retryAfter = headers.getFirst("retry-after");
            if (retryAfter != null) {
                try {
                    return Math.min(BACKOFF_MAXIMO_MS, (long) (Double.parseDouble(retryAfter.trim()) * 1000));
                } catch (NumberFormatException ignorado) {
                    // formato de data HTTP: cai no backoff exponencial
                }
            }
        }
        long exponencial = backoffInicialMs * (1L << (tentativa - 1));
        long jitter = backoffInicialMs == 0 ? 0 : ThreadLocalRandom.current().nextLong(backoffInicialMs);
        return Math.min(BACKOFF_MAXIMO_MS, exponencial + jitter);
    }

    private void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaSobrecarregadaException(provedor);
        }
    }
}
