package com.restoria.analise.dto;

/**
 * @param uploadVendasId  id de um upload de planilha de vendas ja processado (RF-07); nullable
 * @param uploadEstoqueId id de um upload de planilha de estoque ja processado (RF-08); nullable
 *
 * Pelo menos um dos dois deve ser informado.
 */
public record GerarRelatorioRequest(Long uploadVendasId, Long uploadEstoqueId) {
}
