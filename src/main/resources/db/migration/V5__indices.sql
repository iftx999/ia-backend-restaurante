-- O Postgres nao cria indice para chave estrangeira automaticamente, e ate
-- aqui o schema so tinha as PKs/uniques. Sem estes indices, cada consulta
-- por usuario/conversa/upload percorre a tabela inteira.
-- Obs: CREATE INDEX (sem CONCURRENTLY) trava escrita na tabela durante a
-- criacao; ok no volume atual. Para tabelas grandes no futuro, usar
-- CREATE INDEX CONCURRENTLY numa migration fora de transacao.

CREATE INDEX IF NOT EXISTS idx_mensagem_chat_conversa_enviada ON mensagem_chat (conversa_id, enviada_em);
CREATE INDEX IF NOT EXISTS idx_conversa_chat_usuario_iniciada ON conversa_chat (usuario_id, iniciada_em DESC);
CREATE INDEX IF NOT EXISTS idx_relatorio_usuario_gerado ON relatorio (usuario_id, gerado_em DESC);
CREATE INDEX IF NOT EXISTS idx_relatorio_upload_vendas ON relatorio (upload_vendas_id);
CREATE INDEX IF NOT EXISTS idx_relatorio_upload_estoque ON relatorio (upload_estoque_id);
CREATE INDEX IF NOT EXISTS idx_indicador_prato_relatorio ON indicador_prato (relatorio_id);
CREATE INDEX IF NOT EXISTS idx_upload_planilha_usuario ON upload_planilha (usuario_id);
CREATE INDEX IF NOT EXISTS idx_item_venda_upload ON item_venda (upload_id);
CREATE INDEX IF NOT EXISTS idx_item_estoque_upload ON item_estoque (upload_id);
CREATE INDEX IF NOT EXISTS idx_imagem_prato_usuario_criada ON imagem_prato (usuario_id, criada_em);
CREATE INDEX IF NOT EXISTS idx_trecho_conhecimento_documento ON trecho_conhecimento (documento_id);

-- Busca vetorial (RAG): HNSW com distancia de cosseno (operador <=>).
-- Exige pgvector >= 0.5; em versao mais antiga so registra um aviso e a
-- busca continua funcionando sem indice (varredura sequencial).
DO $$
BEGIN
    CREATE INDEX IF NOT EXISTS idx_trecho_conhecimento_embedding
        ON trecho_conhecimento USING hnsw (embedding vector_cosine_ops);
EXCEPTION WHEN others THEN
    RAISE NOTICE 'Indice HNSW nao criado (pgvector sem suporte a hnsw?): %', SQLERRM;
END $$;
