# Geração/edição de imagem de prato via IA (OpenAI)

> Status: **Planejado, aguardando `OPENAI_API_KEY`**. Não implementar antes
> disso — ver `04-roadmap.md`. Este doc é o plano técnico completo para quando
> a chave chegar. Depende de decisão de negócio: valor cobrado/cota (ver §6).

## 1. Objetivo

Permitir que o usuário gere ou edite fotos de prato dentro do próprio chat:
descreve o prato (ou anexa uma foto existente) e recebe imagens prontas para
cardápio, iFood e redes sociais — a feature de "fotos com IA" já anunciada na
landing page.

**Importante — isso não é o mesmo trabalho do `docs/05-multi-provedor-ia.md`.**
Aquele doc troca o modelo de *texto* (Claude ↔ GPT) no chat consultivo; nenhum
dos dois gera imagem por ali. Geração/edição de imagem é uma API completamente
separada da OpenAI (`/v1/images/*`, modelo GPT Image 2.5), com contrato,
custo e fluxo de dados próprios. Os dois podem ser implementados em qualquer
ordem, independentemente.

## 2. Como a API da OpenAI funciona aqui

Dois endpoints relevantes (REST puro, sem SDK — mesmo padrão usado hoje pro
Claude, `RestClient` cru):

- `POST /v1/images/generations` — gera uma imagem nova a partir de um prompt
  de texto. Corpo: `{"model": "gpt-image-2.5-flare", "prompt": "...", "size": "1024x1024", "n": 1}`.
- `POST /v1/images/edits` — edita uma imagem existente a partir de um prompt
  (o caso "usuário anexou uma foto do prato e quer trocar o fundo/iluminação").
  Multipart: campo `image` (a foto original, já temos isso — é o
  `pendingImage`/`imagemBase64` que o chat já sabe enviar) + `prompt` + opcional
  `mask` (área a alterar; sem máscara, o modelo decide a região com base no
  prompt).

Resposta: array de imagens em base64 (`b64_json`) ou URL temporária,
dependendo do parâmetro `response_format`. Recomendação: pedir `b64_json` —
evita depender de uma URL da OpenAI expirar antes de persistirmos a imagem.

Os atalhos de prompt já adicionados no chat (`chat.component.ts`,
`categoriasAtalhosPrompt`) alimentam exatamente o campo `prompt` dessas duas
chamadas — nenhuma mudança neles é necessária quando isso for implementado.

## 3. Design — Backend

### 3.1 Isolamento (regra do `CLAUDE.md`)

Mesma regra do `AiConsultantClient`: nenhuma chamada externa direto de um
Service. Novo pacote `com.restoria.imagem`, com o client isolado em
`integration.ai`:

```java
package com.restoria.integration.ai;

public interface ImagemIaClient {
    ImagemGerada gerar(String prompt, TamanhoImagem tamanho);
    ImagemGerada editar(String prompt, byte[] imagemOriginal, String mediaTypeOriginal);
}
```

- `OpenAiImagemClient` — implementação real (`RestClient`, `restoria.openai.api-key`
  — mesma property já prevista em `docs/05-multi-provedor-ia.md` §3.6, reaproveitada
  aqui; essa feature também depende de `OPENAI_API_KEY`, mesmo sem depender do
  resto daquele doc).
- `MockImagemIaClient` — ativado por `restoria.ai.mock=true` (mesma flag já usada
  pro Claude), devolve uma imagem placeholder fixa em dev local, sem gastar
  crédito.

### 3.2 Fluxo e persistência

```
com.restoria.imagem
├── ImagemPratoController   → POST /api/imagens/gerar, POST /api/imagens/editar
├── ImagemPratoService      → valida limite de uso, chama ImagemIaClient, persiste
├── ImagemPrato (entity)    → ver modelo de dados abaixo
├── ImagemPratoRepository
└── dto/ (GerarImagemRequest, EditarImagemRequest, ImagemPratoResponse)
```

`ImagemPratoService`:
1. `LimiteUsoService.verificarLimiteImagem(usuario)` (novo método, mesmo padrão
   de `verificarLimiteMensagem`/`verificarLimiteRelatorio`).
2. Chama `imagemIaClient.gerar(...)` ou `.editar(...)`.
3. Salva o binário da imagem retornada em storage (ver §3.3) e persiste a
   linha `ImagemPrato` com a referência.
4. Retorna URL da imagem salva pro frontend exibir.

### 3.3 Storage — não guardar binário no Postgres

Guardar imagem como `bytea` no banco funciona mas não escala e complica
backup. Duas opções, recomendação **A**:

- **A) Disco local + servir via endpoint estático** — mais simples pro estágio
  atual (single-tenant, sem infra de nuvem ainda). Salvar em
  `uploads/imagens-prato/{usuarioId}/{uuid}.png`, servir via
  `GET /api/imagens/{id}/arquivo` (`ImagemPratoController`, `Resource`/`InputStreamResource`).
  Migrar pra S3/object storage quando o deploy for multi-instância (hoje é
  Railway, single dyno — ver `railway.toml`).
- B) Object storage (S3/R2/Cloudflare) desde já — melhor a longo prazo, mas
  adiciona uma dependência de infra nova antes de precisar dela.

### 3.4 Modelo de dados

Novo campo no glossário do `docs/03-modelo-dados.md`:

### `ImagemPrato`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| usuario | Usuario (FK) | |
| prompt | Text | prompt final enviado à OpenAI (inclui os atalhos escolhidos) |
| tipoOperacao | Enum (GERACAO, EDICAO) | |
| imagemOriginalCaminho | String | nulo se `GERACAO`; caminho da foto anexada se `EDICAO` |
| caminhoArquivo | String | onde a imagem gerada foi salva (storage, §3.3) |
| criadaEm | LocalDateTime | |

### 3.5 Configuração (`application.yml`)

```yaml
restoria:
  openai:
    api-key: ${OPENAI_API_KEY:}     # mesma property de docs/05, reaproveitada
    imagens:
      modelo-geracao: gpt-image-2.5-flare
      modelo-edicao: gpt-image-2.5-sunburst
      tamanho-padrao: 1024x1024
      timeout-seconds: 60           # geração de imagem é mais lenta que chat
```

### 3.6 Erros e limites da própria API

- Timeout maior que o do chat (geração de imagem demora mais — 10-30s é comum).
- Moderação: a OpenAI recusa prompts que violem a política de conteúdo — mapear
  esse erro pra uma mensagem clara ("não foi possível gerar essa imagem, tente
  reformular o pedido"), não como erro genérico 500.
- Rate limit por conta — mesmo tratamento de `AiConsultantException` hoje
  (retry não automático, erro claro pro usuário).

## 4. Design — Frontend

- Reaproveita o chat existente: usuário anexa a foto (já implementado,
  `pendingImage`) e escreve o prompt com ajuda dos atalhos já adicionados
  (`prompt-atalhos` em `chat.component.html`).
- Novo botão "Gerar imagem" (ou detectar intenção automaticamente quando há
  imagem anexada + texto — a definir na hora, mais simples começar com botão
  explícito pra não confundir com uma pergunta normal de texto).
- Resposta: em vez de bolha de texto, uma bolha com a imagem gerada
  (`message.imageDataUrl` já existe no model — reutilizável) + botão de
  download.
- Estado de carregamento mais longo que o chat de texto (a chamada demora
  mais) — usar o mesmo indicador de "digitando" já existente, mas talvez com
  texto tipo "Gerando imagem...".

## 5. Testes

- `OpenAiImagemClientTest` — mock do `RestClient`, mesmo padrão dos testes
  existentes do Anthropic.
- `ImagemPratoServiceTest` — cobre limite de uso excedido, sucesso de
  geração/edição, e falha da API externa mapeada corretamente.
- Teste manual: gerar e editar com prompts reais usando os atalhos do chat,
  conferir tempo de resposta e qualidade antes de liberar pro gerente real.

## 6. Decisões de negócio em aberto (resolver antes de codar)

- **Cota por plano**: imagem custa mais por chamada que uma mensagem de chat —
  não faz sentido usar a mesma cota de `Plano.mensagensPorMes`. Precisa de um
  novo campo (`imagensPorMes`) no enum `Plano`, com valores a definir (ex:
  GRATIS não inclui, PRO inclui um número limitado por mês).
- **Preço do plano PRO** definido em R$ 49,90/mês (`docs/00-visao-geral.md`
  §3.1) sem essa feature incluída ainda — pode precisar de revisão (ou de um
  add-on separado) quando a geração de imagem entrar de vez, pra cobrir o
  custo adicional por chamada.
- Vale considerar deixar geração de imagem **exclusiva do plano PRO** desde o
  início, dado o custo por chamada ser maior que o de uma mensagem de texto.

## 7. Ordem de implementação sugerida (quando a chave chegar)

1. `ImagemIaClient` + `OpenAiImagemClient` (só geração, sem edição) + `MockImagemIaClient`
2. Storage local (§3.3 opção A) + `ImagemPrato` entity/repository/migração
3. `ImagemPratoService` + `ImagemPratoController` (`POST /api/imagens/gerar`)
4. Edição (`POST /api/imagens/editar`, usa a imagem já anexada no chat)
5. `LimiteUsoService.verificarLimiteImagem` + campo `imagensPorMes` no `Plano`
6. Frontend: botão "Gerar imagem", bolha de resposta com imagem, download
7. Testar com prompts reais (inclusive os atalhos já implementados) e ajustar
   qualidade/custo antes de liberar pro gerente real

## 8. O que falta do usuário

- `OPENAI_API_KEY` (conta OpenAI com billing ativo — mesma chave de `docs/05`,
  serve pras duas features)
- Decisão de negócio da §6 (cota por plano, se PRO-only)
