package com.restoria.assinatura;

import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que requisicoes simultaneas do mesmo usuario nao passam do limite
 * do plano (antes, todas liam a mesma contagem e passavam juntas).
 */
@SpringBootTest
class LimiteUsoServiceConcorrenciaTest {

    @Autowired
    private LimiteUsoService limiteUsoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsoMensalRepository usoMensalRepository;

    @Test
    void reservasSimultaneasNaoUltrapassamOLimiteDoPlano() throws Exception {
        Usuario usuario = usuarioRepository.save(
                new Usuario("Concorrente", "concorrente-" + System.nanoTime() + "@teste.com", "hash", "Restaurante"));
        int limite = Plano.GRATIS.getMensagensPorMes();
        int tentativas = limite * 3;

        AtomicInteger aceitas = new AtomicInteger();
        AtomicInteger bloqueadas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            List<Future<?>> futuros = new ArrayList<>();
            for (int i = 0; i < tentativas; i++) {
                futuros.add(executor.submit(() -> {
                    largada.await();
                    try {
                        limiteUsoService.reservar(usuario, TipoUso.MENSAGEM);
                        aceitas.incrementAndGet();
                    } catch (LimiteUsoExcedidoException e) {
                        bloqueadas.incrementAndGet();
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> futuro : futuros) {
                futuro.get();
            }
        }

        assertThat(aceitas.get()).isEqualTo(limite);
        assertThat(bloqueadas.get()).isEqualTo(tentativas - limite);
        assertThat(usoMensalRepository.findByUsuarioIdAndCompetencia(usuario.getId(), LimiteUsoService.competenciaAtual()))
                .get().extracting(UsoMensal::getMensagens).isEqualTo(limite);
    }
}
