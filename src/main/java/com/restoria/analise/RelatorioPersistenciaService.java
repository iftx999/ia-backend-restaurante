package com.restoria.analise;

import com.restoria.shared.Usuario;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Persistencia do {@link Relatorio} gerado, isolada num bean proprio (em vez
 * de metodo de {@link AnaliseService}) pelo mesmo motivo documentado em
 * {@code ConversaHistoricoService}: {@code @Transactional} so funciona via o
 * proxy do Spring, e a chamada a IA em {@code AnaliseService.gerarRelatorio}
 * precisa acontecer fora de uma transacao (nao prender o pool do HikariCP
 * durante a chamada de rede).
 */
@Component
class RelatorioPersistenciaService {

    private final RelatorioRepository relatorioRepository;

    RelatorioPersistenciaService(RelatorioRepository relatorioRepository) {
        this.relatorioRepository = relatorioRepository;
    }

    @Transactional
    Relatorio persistir(
            Usuario usuario, UploadPlanilha uploadVendas, UploadPlanilha uploadEstoque,
            ResultadoAnalise resultado, String textoIa) {
        Relatorio relatorio = new Relatorio(usuario, uploadVendas, uploadEstoque, resultado.cmvCalculado(), textoIa);

        for (IndicadorPratoCalculado indicador : resultado.indicadoresPrato()) {
            BigDecimal margem = indicador.margemPercentual() == null ? BigDecimal.ZERO : indicador.margemPercentual();
            relatorio.adicionarIndicadorPrato(new IndicadorPrato(indicador.nomePrato(), margem, indicador.alertaMargem()));
        }

        return relatorioRepository.save(relatorio);
    }
}
