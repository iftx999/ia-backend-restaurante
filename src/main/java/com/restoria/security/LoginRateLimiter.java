package com.restoria.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava tentativas de login por e-mail apos varias falhas seguidas —
 * mitigacao simples contra forca bruta de senha (levantado em revisao de
 * seguranca, 2026-09-13).
 *
 * Estado em memoria (ConcurrentHashMap): suficiente pra uma instancia so do
 * backend (ver railway.toml, single dyno hoje). Se o deploy virar
 * multi-instancia no futuro, isso precisa migrar pra um store compartilhado
 * (ex: Redis) — cada instancia teria seu proprio contador do jeito que esta.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    /** Acima disso, cada falha registrada varre e remove as entradas expiradas. */
    private static final int TAMANHO_PARA_LIMPEZA = 10_000;

    private final ConcurrentHashMap<String, Tentativas> tentativasPorEmail = new ConcurrentHashMap<>();

    /** @throws MuitasTentativasException se o e-mail ja bateu o limite de falhas na janela atual. */
    public void verificarLiberado(String email) {
        String chave = normalizar(email);
        Tentativas tentativas = tentativasPorEmail.get(chave);
        if (tentativas == null) {
            return;
        }
        if (tentativas.expirou()) {
            tentativasPorEmail.remove(chave, tentativas);
            return;
        }
        if (tentativas.excedeuLimite()) {
            throw new MuitasTentativasException();
        }
    }

    /**
     * Sem limpeza, e-mails que so falharam (nunca logaram com sucesso) ficariam
     * no mapa para sempre — crescimento sem limite de memoria, ex: num ataque
     * que testa milhares de e-mails diferentes.
     */
    public void registrarFalha(String email) {
        if (tentativasPorEmail.size() > TAMANHO_PARA_LIMPEZA) {
            tentativasPorEmail.values().removeIf(Tentativas::expirou);
        }

        tentativasPorEmail.compute(normalizar(email), (chave, atual) -> {
            if (atual == null || atual.expirou()) {
                return new Tentativas(1, Instant.now());
            }
            return new Tentativas(atual.contagem() + 1, atual.primeiraEm());
        });
    }

    public void registrarSucesso(String email) {
        tentativasPorEmail.remove(normalizar(email));
    }

    private String normalizar(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private record Tentativas(int contagem, Instant primeiraEm) {
        boolean expirou() {
            return Instant.now().isAfter(primeiraEm.plus(JANELA));
        }

        boolean excedeuLimite() {
            return !expirou() && contagem >= MAX_TENTATIVAS;
        }
    }
}
