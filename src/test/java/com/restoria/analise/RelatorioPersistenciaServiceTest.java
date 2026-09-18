package com.restoria.analise;

import com.restoria.shared.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioPersistenciaServiceTest {

    @Mock
    private RelatorioRepository relatorioRepository;

    private RelatorioPersistenciaService relatorioPersistenciaService;
    private final IndicadorCalculator indicadorCalculator = new IndicadorCalculator();
    private final Usuario usuario = new Usuario();

    @BeforeEach
    void setUp() {
        relatorioPersistenciaService = new RelatorioPersistenciaService(relatorioRepository);
        when(relatorioRepository.save(org.mockito.ArgumentMatchers.any(Relatorio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void relatorioSemAnomaliaNaoFicaMarcadoComoAlerta() {
        ResultadoAnalise resultado = indicadorCalculator.calcular(
                List.of(venda("Feijoada", 10, "50.00", "20.00")), List.of());

        Relatorio relatorio = relatorioPersistenciaService.persistir(usuario, null, null, resultado, "texto");

        assertThat(relatorio.isTemAlerta()).isFalse();
        assertThat(relatorio.getQuantidadeAlertas()).isZero();
    }

    @Test
    void relatorioComMargemBaixaFicaMarcadoComoAlerta() {
        ResultadoAnalise resultado = indicadorCalculator.calcular(
                List.of(venda("Prato caro", 5, "30.00", "27.00")), List.of());

        Relatorio relatorio = relatorioPersistenciaService.persistir(usuario, null, null, resultado, "texto");

        assertThat(relatorio.isTemAlerta()).isTrue();
        assertThat(relatorio.getQuantidadeAlertas()).isEqualTo(1);
    }

    @Test
    void relatorioComMargemBaixaEPerdaAltaContaAsDuasAnomalias() {
        ResultadoAnalise resultado = indicadorCalculator.calcular(
                List.of(venda("Prato caro", 5, "30.00", "27.00")),
                List.of(estoque("Farinha", "100", "2.50", "20")));

        Relatorio relatorio = relatorioPersistenciaService.persistir(usuario, null, null, resultado, "texto");

        assertThat(relatorio.isTemAlerta()).isTrue();
        assertThat(relatorio.getQuantidadeAlertas()).isEqualTo(2);
    }

    private ItemVenda venda(String prato, int quantidade, String precoVenda, String custoUnitario) {
        ItemVenda item = new ItemVenda();
        item.setNomePrato(prato);
        item.setQuantidadeVendida(quantidade);
        item.setPrecoVenda(new BigDecimal(precoVenda));
        item.setCustoUnitario(custoUnitario == null ? null : new BigDecimal(custoUnitario));
        return item;
    }

    private ItemEstoque estoque(String insumo, String quantidadeComprada, String custoUnitario, String quantidadePerdida) {
        ItemEstoque item = new ItemEstoque();
        item.setNomeInsumo(insumo);
        item.setQuantidadeComprada(new BigDecimal(quantidadeComprada));
        item.setCustoUnitario(new BigDecimal(custoUnitario));
        item.setQuantidadePerdida(quantidadePerdida == null ? null : new BigDecimal(quantidadePerdida));
        return item;
    }
}
