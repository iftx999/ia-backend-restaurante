-- Troca as colunas `oid` (large objects) por `text`.
--  * large object e lido por uma API separada (uma ida ao banco por mensagem)
--    e exige transacao aberta ("Unable to access lob stream");
--  * o large object NAO e apagado junto com a linha: sem lo_unlink, o banco
--    acumula lixo (pg_largeobject) para sempre.
-- Os objetos antigos sao desvinculados (lo_unlink) depois de copiados.

ALTER TABLE mensagem_chat ADD COLUMN conteudo_texto text;
UPDATE mensagem_chat SET conteudo_texto = convert_from(lo_get(conteudo), 'UTF8');
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT conteudo FROM mensagem_chat LOOP
        PERFORM lo_unlink(r.conteudo);
    END LOOP;
END $$;
ALTER TABLE mensagem_chat DROP COLUMN conteudo;
ALTER TABLE mensagem_chat RENAME COLUMN conteudo_texto TO conteudo;
ALTER TABLE mensagem_chat ALTER COLUMN conteudo SET NOT NULL;

ALTER TABLE trecho_conhecimento ADD COLUMN conteudo_texto text;
UPDATE trecho_conhecimento SET conteudo_texto = convert_from(lo_get(conteudo), 'UTF8');
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT conteudo FROM trecho_conhecimento LOOP
        PERFORM lo_unlink(r.conteudo);
    END LOOP;
END $$;
ALTER TABLE trecho_conhecimento DROP COLUMN conteudo;
ALTER TABLE trecho_conhecimento RENAME COLUMN conteudo_texto TO conteudo;
ALTER TABLE trecho_conhecimento ALTER COLUMN conteudo SET NOT NULL;

-- Depois da troca acima nenhuma coluna do schema referencia large objects:
-- o que sobrou em pg_largeobject e lixo de linhas apagadas no passado.
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT oid FROM pg_largeobject_metadata LOOP
        PERFORM lo_unlink(r.oid);
    END LOOP;
END $$;

-- Titulo da conversa (primeira pergunta do usuario) gravado na propria
-- conversa: a sidebar deixa de fazer uma consulta extra por conversa (N+1).
ALTER TABLE conversa_chat ADD COLUMN titulo character varying(200);
UPDATE conversa_chat c
SET titulo = LEFT(m.conteudo, 200)
FROM (
    SELECT DISTINCT ON (conversa_id) conversa_id, conteudo
    FROM mensagem_chat
    WHERE autor = 'USUARIO'
    ORDER BY conversa_id, enviada_em
) m
WHERE m.conversa_id = c.id;
