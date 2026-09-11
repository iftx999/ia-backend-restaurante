package com.restoria.analise;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converte as linhas de uma planilha de estoque/insumos em {@link ItemEstoque} (RF-08).
 */
@Component
class EstoquePlanilhaMapeador {

    private final PlanilhaLeitor leitor;

    EstoquePlanilhaMapeador(PlanilhaLeitor leitor) {
        this.leitor = leitor;
    }

    List<ItemEstoque> mapear(MultipartFile arquivo, UploadPlanilha upload) {
        List<Map<String, String>> linhas = leitor.ler(arquivo);
        if (linhas.isEmpty()) {
            throw new PlanilhaInvalidaException("Planilha de estoque nao contem nenhuma linha de dados");
        }

        List<ItemEstoque> itens = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            Map<String, String> linha = linhas.get(i);
            int numeroLinha = i + 2;

            try {
                String nomeInsumo = PlanilhaColunaUtil.texto(linha, "insumo", "nome_insumo", "item");
                if (nomeInsumo == null) {
                    erros.add("Linha " + numeroLinha + ": coluna \"insumo\" (nome do insumo) ausente ou vazia");
                    continue;
                }

                BigDecimal quantidadeComprada = PlanilhaColunaUtil.numero(linha, "quantidade", "quantidade_comprada", "qtd");
                if (quantidadeComprada == null) {
                    erros.add("Linha " + numeroLinha + ": coluna \"quantidade\" ausente ou vazia");
                    continue;
                }

                BigDecimal custoUnitario = PlanilhaColunaUtil.numero(linha, "custo", "custo_unitario");
                if (custoUnitario == null) {
                    erros.add("Linha " + numeroLinha + ": coluna \"custo\" (custo unitario) ausente ou vazia");
                    continue;
                }

                BigDecimal quantidadePerdida = PlanilhaColunaUtil.numero(linha, "perda", "quantidade_perdida");
                LocalDate data = PlanilhaColunaUtil.data(linha, "data");

                itens.add(new ItemEstoque(upload, nomeInsumo, quantidadeComprada, custoUnitario, quantidadePerdida, data));
            } catch (PlanilhaInvalidaException e) {
                erros.add("Linha " + numeroLinha + ": " + e.getMessage());
            }
        }

        if (!erros.isEmpty()) {
            throw new PlanilhaInvalidaException(erros);
        }
        return itens;
    }
}
