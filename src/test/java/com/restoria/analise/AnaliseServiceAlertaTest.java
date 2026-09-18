package com.restoria.analise;

import com.restoria.analise.dto.AlertaRelatorioResponse;
import com.restoria.assinatura.LimiteUsoService;
import com.restoria.integration.ai.AiConsultantClient;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** RF-16: {@code GET /api/analise/relatorio/alerta}, ver {@code AnaliseController}. */
@ExtendWith(MockitoExtension.class)
class AnaliseServiceAlertaTest {

    @Mock
    private UploadPlanilhaRepository uploadPlanilhaRepository;
    @Mock
    private ItemVendaRepository itemVendaRepository;
    @Mock
    private ItemEstoqueRepository itemEstoqueRepository;
    @Mock
    private RelatorioRepository relatorioRepository;
    @Mock
    private VendasPlanilhaMapeador vendasPlanilhaMapeador;
    @Mock
    private EstoquePlanilhaMapeador estoquePlanilhaMapeador;
    @Mock
    private IndicadorCalculator indicadorCalculator;
    @Mock
    private AiConsultantClient aiConsultantClient;
    @Mock
    private UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    @Mock
    private RelatorioPersistenciaService relatorioPersistenciaService;
    @Mock
    private LimiteUsoService limiteUsoService;

    private AnaliseService analiseService;
    private final Usuario usuario = new Usuario();

    @BeforeEach
    void setUp() {
        analiseService = new AnaliseService(
                uploadPlanilhaRepository, itemVendaRepository, itemEstoqueRepository, relatorioRepository,
                vendasPlanilhaMapeador, estoquePlanilhaMapeador, indicadorCalculator, aiConsultantClient,
                usuarioAutenticadoProvider, relatorioPersistenciaService, limiteUsoService);
        when(usuarioAutenticadoProvider.obterAtual()).thenReturn(usuario);
    }

    @Test
    void retornaSemAlertaQuandoUsuarioAindaNaoGerouNenhumRelatorio() {
        when(relatorioRepository.findFirstByUsuarioOrderByGeradoEmDesc(usuario)).thenReturn(Optional.empty());

        AlertaRelatorioResponse resposta = analiseService.obterAlertaMaisRecente();

        assertThat(resposta.temAlerta()).isFalse();
        assertThat(resposta.relatorioId()).isNull();
    }

    @Test
    void retornaAlertaDoRelatorioMaisRecenteQuandoExiste() {
        Relatorio relatorio = new Relatorio(usuario, null, null, new BigDecimal("0.3500"), "texto");
        relatorio.setId(42L);
        relatorio.setTemAlerta(true);
        relatorio.setQuantidadeAlertas(2);
        when(relatorioRepository.findFirstByUsuarioOrderByGeradoEmDesc(usuario)).thenReturn(Optional.of(relatorio));

        AlertaRelatorioResponse resposta = analiseService.obterAlertaMaisRecente();

        assertThat(resposta.temAlerta()).isTrue();
        assertThat(resposta.relatorioId()).isEqualTo(42L);
        assertThat(resposta.quantidadeAlertas()).isEqualTo(2);
        assertThat(resposta.geradoEm()).isInstanceOf(LocalDateTime.class);
    }
}
