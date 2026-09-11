package com.restoria.analise;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converte as linhas de uma planilha de vendas em {@link ItemVenda} (RF-07).
 */
@Component
class VendasPlanilhaMapeador {

    private final PlanilhaLeitor leitor;

    VendasPlanilhaMapeador(PlanilhaLeitor leitor) {
        this.leitor = leitor;
    }

    List<ItemVenda> mapear(MultipartFile arquivo, UploadPlanilha upload) {
        List<Map<String, String>> linhas = leitor.ler(arquivo);
        if (linhas.isEmpty()) {
            throw new PlanilhaInvalidaException("Planilha de vendas nao contem nenhuma linha de dados");
        }

        List<ItemVenda> itens = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            Map<String, String> linha = linhas.get(i);
            int numeroLinha = i + 2; // +1 cabecalho, +1 indice 1-based

            try {
                String nomePrato = PlanilhaColunaUtil.texto(linha, "prato", "nome_prato", "item");
                if (nomePrato == null) {
                    erros.add("Linha " + numeroLinha + ": coluna \"prato\" (nome do prato) ausente ou vazia");
                    continue;
                }

                Integer quantidade = PlanilhaColunaUtil.inteiro(linha, "quantidade", "quantidade_vendida", "qtd");
                if (quantidade == null) {
                    erros.add("Linha " + numeroLinha + ": coluna \"quantidade\" ausente ou vazia");
                    continue;
                }

                BigDecimal precoVenda = PlanilhaColunaUtil.numero(linha, "preco", "preco_venda", "valor");
                if (precoVenda == null) {
                    erros.add("Linha " + numeroLinha + ": coluna \"preco\" (preco de venda) ausente ou vazia");
                    continue;
                }

                BigDecimal custoUnitario = PlanilhaColunaUtil.numero(linha, "custo", "custo_unitario");
                LocalDate data = PlanilhaColunaUtil.data(linha, "data");

                itens.add(new ItemVenda(upload, nomePrato, quantidade, precoVenda, custoUnitario, data));
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
