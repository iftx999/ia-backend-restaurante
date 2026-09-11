package com.restoria.conhecimento;

/**
 * Trecho recuperado da base de conhecimento por similaridade (RAG), pronto
 * para ser usado como contexto adicional no prompt da IA.
 */
public record TrechoRelevante(String conteudo, String tituloDocumento) {
}
