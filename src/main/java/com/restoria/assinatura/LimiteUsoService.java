package com.restoria.assinatura;

import com.restoria.analise.RelatorioRepository;
import com.restoria.chat.AutorMensagem;
import com.restoria.chat.MensagemChatRepository;
import com.restoria.imagem.ImagemPratoRepository;
import com.restoria.shared.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Impede que o uso da API da Anthropic (custo direto) fique ilimitado por
 * usuario. Nao mantem contador proprio: conta direto em cima dos timestamps
 * que MensagemChat/Relatorio ja tem, filtrando pelo mes corrente — evita o
 * risco de um contador dessincronizar do dado real.
 */
@Service
public class LimiteUsoService {

    private final AssinaturaRepository assinaturaRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final RelatorioRepository relatorioRepository;
    private final ImagemPratoRepository imagemPratoRepository;

    public LimiteUsoService(
            AssinaturaRepository assinaturaRepository,
            MensagemChatRepository mensagemChatRepository,
            RelatorioRepository relatorioRepository,
            ImagemPratoRepository imagemPratoRepository) {
        this.assinaturaRepository = assinaturaRepository;
        this.mensagemChatRepository = mensagemChatRepository;
        this.relatorioRepository = relatorioRepository;
        this.imagemPratoRepository = imagemPratoRepository;
    }

    /** @throws LimiteUsoExcedidoException se o usuario ja bateu o limite de mensagens do mes no plano atual. */
    public void verificarLimiteMensagem(Usuario usuario) {
        Plano plano = planoAtual(usuario);
        long enviadasNoMes = contarMensagensNoMes(usuario);

        if (enviadasNoMes >= plano.getMensagensPorMes()) {
            throw new LimiteUsoExcedidoException(
                    "Voce atingiu o limite de " + plano.getMensagensPorMes() + " mensagens do plano " + plano
                            + " neste mes. Faca upgrade para continuar.");
        }
    }

    /** @throws LimiteUsoExcedidoException se o usuario ja bateu o limite de relatorios do mes no plano atual. */
    public void verificarLimiteRelatorio(Usuario usuario) {
        Plano plano = planoAtual(usuario);
        long geradosNoMes = contarRelatoriosNoMes(usuario);

        if (geradosNoMes >= plano.getRelatoriosPorMes()) {
            throw new LimiteUsoExcedidoException(
                    "Voce atingiu o limite de " + plano.getRelatoriosPorMes() + " relatorios do plano " + plano
                            + " neste mes. Faca upgrade para continuar.");
        }
    }

    /**
     * @throws LimiteUsoExcedidoException se o usuario ja bateu o limite de imagens do mes no plano
     *         atual, ou se o plano nao inclui geracao de imagem (GRATIS: {@code imagensPorMes = 0},
     *         qualquer chamada cai nesse limite — ver docs/06-geracao-imagem-ia.md, secao 6)
     */
    public void verificarLimiteImagem(Usuario usuario) {
        Plano plano = planoAtual(usuario);
        long geradasNoMes = contarImagensNoMes(usuario);

        if (geradasNoMes >= plano.getImagensPorMes()) {
            String mensagem = plano.getImagensPorMes() == 0
                    ? "Geracao de imagem de prato e exclusiva do plano PRO. Faca upgrade para usar essa funcionalidade."
                    : "Voce atingiu o limite de " + plano.getImagensPorMes() + " imagens do plano " + plano
                            + " neste mes. Faca upgrade para continuar.";
            throw new LimiteUsoExcedidoException(mensagem);
        }
    }

    /** Resumo de uso do mes corrente, usado por {@code AssinaturaController} pra exibir "3/20 mensagens" etc. */
    public ResumoUso resumoUso(Usuario usuario) {
        Plano plano = planoAtual(usuario);
        return new ResumoUso(plano, contarMensagensNoMes(usuario), contarRelatoriosNoMes(usuario));
    }

    private long contarMensagensNoMes(Usuario usuario) {
        return mensagemChatRepository.countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
                usuario.getId(), AutorMensagem.USUARIO, inicioDoMes(), fimDoMes());
    }

    private long contarRelatoriosNoMes(Usuario usuario) {
        return relatorioRepository.countByUsuarioAndGeradoEmBetween(usuario, inicioDoMes(), fimDoMes());
    }

    private long contarImagensNoMes(Usuario usuario) {
        return imagemPratoRepository.countByUsuarioAndCriadaEmBetween(usuario, inicioDoMes(), fimDoMes());
    }

    /**
     * So aplica os limites do plano pago enquanto a assinatura estiver ATIVA.
     * INADIMPLENTE/CANCELADA cai pro plano gratis ate o pagamento ser regularizado
     * (o {@code status} so muda via webhook do Stripe, ver {@code AssinaturaService}).
     */
    private Plano planoAtual(Usuario usuario) {
        return assinaturaRepository.findByUsuario(usuario)
                .filter(assinatura -> assinatura.getStatus() == StatusAssinatura.ATIVA)
                .map(Assinatura::getPlano)
                .orElse(Plano.GRATIS);
    }

    public record ResumoUso(Plano plano, long mensagensNoMes, long relatoriosNoMes) {
    }

    private LocalDateTime inicioDoMes() {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    private LocalDateTime fimDoMes() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
    }
}
