# Conteúdo-semente da base de conhecimento (RAG)

Documentos de boas práticas de gestão de restaurante, escritos para alimentar a
base de conhecimento do modo consultivo (ver `docs/01-requisitos-funcionais.md`
RF-19/20/21 e `docs/04-roadmap.md`, seção "Base de Conhecimento (RAG)").

Cobertura (mesmos temas do system prompt em `RestoriaSystemPrompt`):
- `01-cmv.md` — CMV, como calcular, faixas de referência, causas de alta, como reduzir
- `02-precificacao-ficha-tecnica.md` — ficha técnica, precificação por markup sobre CMV alvo
- `03-controle-estoque.md` — FIFO, contagem, giro de estoque, ponto de pedido
- `04-compliance-sanitario.md` — RDC 216/ANVISA, temperatura, higiene, documentação
- `05-gestao-pessoas.md` — escala, treinamento, retenção, indicadores de turnover

## Como ingerir (quando o endpoint estiver disponível)

Pré-requisitos: extensão `pgvector` instalada no Postgres, `VOYAGE_API_KEY`
configurada, endpoint `POST /api/conhecimento/documentos` no ar.

Cada arquivo pode ser enviado assim (exemplo com `curl` + `jq` para montar o JSON):

```bash
for arquivo in src/main/resources/conhecimento-seed/0*.md; do
  titulo=$(head -n1 "$arquivo")
  conteudo=$(tail -n +2 "$arquivo")
  jq -n --arg titulo "$titulo" --arg conteudo "$conteudo" --arg fonte "$(basename "$arquivo")" \
    '{titulo: $titulo, conteudo: $conteudo, fonte: $fonte}' | \
    curl -s -X POST http://localhost:8080/api/conhecimento/documentos \
      -H "Content-Type: application/json" -d @-
  echo
done
```

Este README não é conteúdo de RAG — não deve ser ingerido.
