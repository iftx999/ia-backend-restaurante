package com.restoria.assinatura;

import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Impede que o uso das APIs de IA (custo direto) fique ilimitado por usuario.
 *
 * <p>Fluxo: {@link #reservar} ANTES da chamada a IA e {@link #estornar} se a
 * chamada falhar. A reserva trava a linha do usuario (SELECT ... FOR UPDATE)
 * numa transacao curta, confere o contador de {@link UsoMensal} contra o
 * limite do plano e ja incrementa — assim N requisicoes simultaneas do mesmo
 * usuario sao serializadas e no maximo {@code limite} delas passam.
 */
@Service
public class LimiteUsoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AssinaturaRepository assinaturaRepository;
    private final UsoMensalRepository usoMensalRepository;
    private final UsuarioRepository usuarioRepository;

    public LimiteUsoService(
            AssinaturaRepository assinaturaRepository,
            UsoMensalRepository usoMensalRepository,
            UsuarioRepository usuarioRepository) {
        this.assinaturaRepository = assinaturaRepository;
        this.usoMensalRepository = usoMensalRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Reserva uma unidade de {@code tipo} no mes corrente.
     *
     * @throws LimiteUsoExcedidoException se o usuario ja bateu o limite do plano atual, ou se o
     *         plano nao inclui o recurso (GRATIS: {@code imagensPorMes = 0} — ver
     *         docs/06-geracao-imagem-ia.md, secao 6)
     */
    @Transactional
    public void reservar(Usuario usuario, TipoUso tipo) {
        Plano plano = planoAtual(usuario);
        int limite = tipo.limiteDo(plano);

        if (limite == 0) {
            throw new LimiteUsoExcedidoException(mensagemRecursoForaDoPlano(tipo, plano));
        }

        usuarioRepository.travarPorId(usuario.getId());
        UsoMensal uso = usoDoMes(usuario.getId());

        if (uso.quantidade(tipo) >= limite) {
            throw new LimiteUsoExcedidoException(
                    "Voce atingiu o limite de " + limite + " " + nome(tipo) + " do plano " + plano
                            + " neste mes. Faca upgrade para continuar.");
        }

        uso.somar(tipo, 1);
        usoMensalRepository.save(uso);
    }

    /** Devolve uma unidade reservada por {@link #reservar} quando a chamada a IA falhou. */
    @Transactional
    public void estornar(Usuario usuario, TipoUso tipo) {
        usuarioRepository.travarPorId(usuario.getId());
        usoMensalRepository.findByUsuarioIdAndCompetencia(usuario.getId(), competenciaAtual())
                .ifPresent(uso -> {
                    uso.somar(tipo, -1);
                    usoMensalRepository.save(uso);
                });
    }

    /** Resumo de uso do mes corrente, usado por {@code AssinaturaController} pra exibir "3/20 mensagens" etc. */
    @Transactional(readOnly = true)
    public ResumoUso resumoUso(Usuario usuario) {
        Plano plano = planoAtual(usuario);
        return usoMensalRepository.findByUsuarioIdAndCompetencia(usuario.getId(), competenciaAtual())
                .map(uso -> new ResumoUso(plano, uso.getMensagens(), uso.getRelatorios()))
                .orElseGet(() -> new ResumoUso(plano, 0, 0));
    }

    private UsoMensal usoDoMes(Long usuarioId) {
        String competencia = competenciaAtual();
        return usoMensalRepository.findByUsuarioIdAndCompetencia(usuarioId, competencia)
                .orElseGet(() -> new UsoMensal(usuarioId, competencia));
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

    private String mensagemRecursoForaDoPlano(TipoUso tipo, Plano plano) {
        if (tipo == TipoUso.IMAGEM) {
            return "Geracao de imagem de prato e exclusiva do plano PRO. Faca upgrade para usar essa funcionalidade.";
        }
        return "O plano " + plano + " nao inclui " + nome(tipo) + ". Faca upgrade para usar essa funcionalidade.";
    }

    private String nome(TipoUso tipo) {
        return switch (tipo) {
            case MENSAGEM -> "mensagens";
            case RELATORIO -> "relatorios";
            case IMAGEM -> "imagens";
        };
    }

    static String competenciaAtual() {
        return YearMonth.now(FUSO).toString();
    }

    public record ResumoUso(Plano plano, long mensagensNoMes, long relatoriosNoMes) {
    }
}
