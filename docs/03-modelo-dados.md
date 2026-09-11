# Modelo de Dados (rascunho inicial)

> Este é um ponto de partida — vai evoluir conforme os requisitos forem refinados.

## Entidades principais

### `Usuario`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| nome | String | |
| email | String | único |
| senha | String | hash (BCrypt) |
| nomeRestaurante | String | |
| criadoEm | LocalDateTime | |

### `ConversaChat`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| usuario | Usuario (FK) | |
| iniciadaEm | LocalDateTime | |

### `MensagemChat`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| conversa | ConversaChat (FK) | |
| autor | Enum (USUARIO, IA) | |
| conteudo | Text | |
| enviadaEm | LocalDateTime | |

### `UploadPlanilha`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| usuario | Usuario (FK) | |
| tipo | Enum (VENDAS, ESTOQUE) | |
| nomeArquivoOriginal | String | |
| status | Enum (PROCESSANDO, PROCESSADO, ERRO) | |
| enviadoEm | LocalDateTime | |

### `ItemVenda` (dado extraído da planilha de vendas)
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| upload | UploadPlanilha (FK) | |
| nomePrato | String | |
| quantidadeVendida | Integer | |
| precoVenda | BigDecimal | |
| custoUnitario | BigDecimal | opcional — adicionado na Fase 2 (não estava no rascunho original); ver decisão abaixo |
| data | LocalDate | opcional |

### `ItemEstoque` (dado extraído da planilha de estoque)
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| upload | UploadPlanilha (FK) | |
| nomeInsumo | String | |
| quantidadeComprada | BigDecimal | |
| custoUnitario | BigDecimal | |
| quantidadePerdida | BigDecimal | opcional, se houver essa info |
| data | LocalDate | |

### `Relatorio`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| usuario | Usuario (FK) | |
| uploadVendas | UploadPlanilha (FK) | nullable |
| uploadEstoque | UploadPlanilha (FK) | nullable |
| cmvCalculado | BigDecimal | |
| conteudoTextoIA | Text | análise gerada pela IA |
| geradoEm | LocalDateTime | |

### `IndicadorPrato` (indicadores calculados por prato, ligados a um Relatorio)
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| relatorio | Relatorio (FK) | |
| nomePrato | String | |
| margemCalculada | BigDecimal | |
| alerta | Boolean | true se margem abaixo do esperado |

### `DocumentoConhecimento` (base de conhecimento para RAG)
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| titulo | String | |
| fonte | String | opcional — origem do documento (ex: nome do arquivo) |
| criadoEm | LocalDateTime | |

### `TrechoConhecimento` (chunk de um documento, com embedding)
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| documento | DocumentoConhecimento (FK) | |
| ordem | Integer | posição do trecho dentro do documento |
| conteudo | Text | |
| embedding | vector(1024) | gerado via Voyage AI (`integration.ai`), extensão `pgvector` do Postgres |

## Relacionamentos (visão simplificada)

```
Usuario 1───N ConversaChat 1───N MensagemChat
Usuario 1───N UploadPlanilha 1───N ItemVenda / ItemEstoque
Usuario 1───N Relatorio 1───N IndicadorPrato
DocumentoConhecimento 1───N TrechoConhecimento
```

## Perguntas em aberto
- [x] Vale a pena normalizar "prato" como entidade própria (com ficha técnica) já na Fase 2, ou só na Fase 3? — **Decisão**: não na Fase 2. Em vez de ficha técnica, a planilha de vendas passou a aceitar uma coluna opcional `custoUnitario` por prato (assume que o gerente já sabe o custo do prato que vende, prática comum em controle de CMV simples). Sem isso não haveria como calcular margem por prato (RF-09) nem detectar anomalias (RF-10). Consequência: sem saldo de estoque (abertura/fechamento), o "giro de estoque" clássico não é calculável — usamos percentual de perda por insumo como proxy (ver `IndicadorCalculator`). Ficha técnica real fica para quando o modelo evoluir (Fase 3+).
- [x] Como lidar com planilhas em formatos muito diferentes entre restaurantes? — **Decisão**: template não é rígido; o parser (`PlanilhaColunaUtil`) aceita alguns nomes alternativos por coluna (ex: "preco"/"preco_venda"/"valor") e aceita tanto CSV quanto XLSX, normalizando acentos e espaços no cabeçalho. Colunas obrigatórias ausentes geram erro claro (RF-13) em vez de falha silenciosa.
