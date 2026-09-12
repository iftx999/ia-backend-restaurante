# Multi-provedor de IA (Claude / GPT)

> Status: **Planejado, aguardando `OPENAI_API_KEY`**. Não implementar antes disso —
> ver `04-roadmap.md`. Este doc é o plano técnico completo para quando a chave chegar.

## 1. Objetivo

Permitir que o usuário escolha, no chat consultivo, se a resposta vem do Claude
(Anthropic) ou do GPT (OpenAI). **Claude continua sendo o padrão** — o usuário
troca manualmente se quiser, por vontade própria (ex: comparar respostas, ou
preferência pessoal). Não é sobre "IA melhor faz X" automático — é uma escolha
explícita do usuário.

Fora de escopo por enquanto: alternância automática por tipo de pergunta,
geração/edição de imagem (nenhum dos dois modelos faz isso via texto no nosso
fluxo atual), suporte a outros provedores (Gemini, Grok etc.) — a arquitetura
abaixo já deixa isso mais fácil de adicionar depois, mas não é o foco agora.

## 2. Por que é viável sem reescrever nada

A arquitetura já isola a chamada de IA atrás da interface `AiConsultantClient`
(`com.restoria.integration.ai`), regra de arquitetura do projeto (`CLAUDE.md`):
> "Chamadas à API da Anthropic ficam isoladas no pacote `integration.ai`. Nunca
> chamar a API diretamente de um Service."

Isso significa que `ChatService` já não sabe (nem deveria saber) qual provedor
está por trás — hoje só existe uma implementação (`AnthropicAiConsultantClient`)
injetada diretamente. A mudança é: em vez de injetar *uma* implementação fixa,
injetar as duas e escolher qual usar por requisição.

## 3. Design — Backend

### 3.1 Novo enum `ModeloIa`

```java
package com.restoria.integration.ai;

public enum ModeloIa {
    CLAUDE, GPT
}
```

### 3.2 Onde a preferência vive

Duas opções — **recomendação: opção A** (mais simples, menos estado):

- **A) Só na mensagem** — `ChatRequest` ganha um campo opcional `modeloIa`
  (`"claude"` | `"gpt"`, default `"claude"` se ausente). O frontend guarda a
  última escolha do usuário em memória/`localStorage` (não precisa persistir
  no backend). Mais simples, sem migração de banco, sem novo endpoint.
- B) Persistida por usuário — campo `Usuario.modeloIaPreferido`, com endpoint
  `PATCH` pra atualizar. Mais "sticky" entre dispositivos, mas exige migração
  de schema e endpoint novo pra uma coisa que é só preferência de sessão.

Ir com **A** primeiro; migrar pra B depois se o usuário real pedir ("quero que
lembre minha escolha entre aparelhos").

### 3.3 `ChatRequest` (`com.restoria.chat.dto`)

```java
public record ChatRequest(
        String conversationId,
        @NotBlank String mensagem,
        String imagemBase64,
        String imagemMediaType,
        String modeloIa   // "claude" (default) | "gpt" — validar/normalizar no service
) { ... }
```
(seguir o mesmo padrão do construtor de 2/4 args já usado para não quebrar os
testes existentes, como fizemos para `imagemBase64`/`imagemMediaType`.)

### 3.4 Roteamento entre os dois clients

```java
package com.restoria.integration.ai;

@Component
public class AiConsultantClientRouter {
    private final AiConsultantClient claudeClient;   // @Qualifier ou tipo concreto
    private final AiConsultantClient gptClient;

    public AiConsultantClient resolver(ModeloIa modelo) {
        return modelo == ModeloIa.GPT ? gptClient : claudeClient;
    }
}
```

`ChatService` passa a depender do router em vez do `AiConsultantClient` direto,
e resolve o client certo antes de chamar `enviarMensagem`/`enviarMensagemStream`.
Como as duas implementações têm a mesma interface, o resto do fluxo
(`ConversaHistoricoService`, persistência, streaming SSE) **não muda nada**.

### 3.5 Novo `OpenAiConsultantClient`

Mesma forma do `AnthropicAiConsultantClient` (RestClient cru, sem SDK), mas
para a API da OpenAI:

- Endpoint: `POST /v1/chat/completions` (mais simples) **ou** `POST /v1/responses`
  (API mais nova, necessária se quisermos busca na web equivalente — ver 3.7).
  Recomendação: ir direto de `/v1/responses`, já que também vamos querer web
  search e é o caminho que a OpenAI está consolidando.
- System prompt: mesmo texto (`RestoriaSystemPrompt.TEXTO`), só muda o formato
  de envio (`instructions` no `/v1/responses`, em vez do campo `system` da
  Anthropic).
- Imagem (upload já implementado no chat): content block
  `{"type": "input_image", "image_url": "data:<mediaType>;base64,<...>"}` em
  vez do formato `source.base64` da Anthropic.
- Streaming: formato de SSE da OpenAI é diferente (eventos tipo
  `response.output_text.delta`, `response.completed` etc.) — precisa de um
  `OpenAiSseStreamProcessor` próprio, espelhando o
  `AnthropicSseStreamProcessor` existente (mesma ideia, parser diferente).
- Erros/timeout: lançar a mesma `AiConsultantException` já tratada pelo
  `GlobalExceptionHandler` — nenhuma mudança ali.

### 3.6 Configuração (`application.yml` / env vars)

```yaml
restoria:
  ai:
    # ... bloco existente do Claude, sem mudanças ...
  openai:
    api-key: ${OPENAI_API_KEY:}
    base-url: https://api.openai.com
    model: gpt-4.1          # definir modelo exato quando a chave chegar
    max-tokens: 2048
    timeout-seconds: 30
    web-search-max-uses: 3   # mesma ideia do restoria.ai.web-search-max-uses
```

Novo `OpenAiProperties` (`@ConfigurationProperties(prefix = "restoria.openai")`),
espelhando `AiProperties`. Chave **nunca** hardcoded — mesma regra do
`ANTHROPIC_API_KEY` (`CLAUDE.md`).

### 3.7 Busca na web (paridade com o que já existe no Claude)

Já implementamos `web_search_20250305` pro Claude (ver commit "Adiciona
ferramenta de busca na web ao chat consultivo"). A OpenAI tem equivalente na
`/v1/responses` API (`tools: [{"type": "web_search"}]`, nome exato a confirmar
na doc oficial no momento da implementação — a API tem evoluído). Sem isso, o
GPT no nosso chat ficaria pior que o Claude pra perguntas tipo "fornecedores em
Limeira" — então **isso não é opcional**, é parte do mesmo nível de qualidade.

### 3.8 Mock (`restoria.ai.mock=true`)

`MockAiConsultantClient` já existe pro Claude (dev local sem gastar crédito).
Precisa de um `MockOpenAiConsultantClient` equivalente, ativado pela mesma
flag, senão dev local do modo GPT não funciona sem gastar crédito de verdade.

## 4. Design — Frontend

- Seletor no header do chat (ao lado do nome "RestorIA" ou perto do input):
  dois botões/segmented control **Claude | GPT**, Claude selecionado por
  padrão. Visual consistente com o resto do redesign (flat, sem gradiente).
- Estado guardado em `localStorage` (`restoria_modelo_ia_preferido`), lido no
  `ChatComponent` na inicialização — sobrevive a refresh, mas não precisa de
  chamada ao backend.
- `ChatRequest`/`chat.service.ts`: adicionar `modeloIa?: 'claude' | 'gpt'` no
  corpo de `POST /api/chat` e `POST /api/chat/stream`.
- Indicar visualmente no balão de resposta qual modelo respondeu (ex: um
  rótulo pequeno "Claude" / "GPT" acima da resposta) — ajuda o usuário a
  comparar quando estiver testando os dois.

## 5. Modelo de dados

Nenhuma migração de schema necessária na opção A (§3.2) — `modeloIa` vive só
na requisição, não é persistido em `MensagemChat`. Se migrarmos pra opção B no
futuro, aí sim precisa de coluna em `Usuario` (mesmo padrão de
`margemMinimaEsperada`/`percentualPerdaAlerta`, ver `docs/03-modelo-dados.md`).

## 6. Testes

- `OpenAiConsultantClientTest` (mock do `RestClient`, mesmo padrão dos testes
  existentes do Anthropic, se houver — senão, teste de integração leve tipo o
  `ChatServiceTest`).
- `OpenAiSseStreamProcessorTest` — espelhar `AnthropicSseStreamProcessorTest`
  com um SSE de exemplo da OpenAI como fixture.
- `ChatServiceTest`: casos novos cobrindo `modeloIa=gpt` roteando pro client
  certo (usar um fake/mock do router).

## 7. Ordem de implementação sugerida (quando a chave chegar)

1. `ModeloIa` enum + `OpenAiProperties` + config no `application.yml`
2. `OpenAiConsultantClient` (mensagem simples, sem streaming, sem imagem, sem web search) + `MockOpenAiConsultantClient`
3. `AiConsultantClientRouter` + `ChatRequest.modeloIa` + `ChatService` usando o router
4. Streaming (`OpenAiSseStreamProcessor`)
5. Suporte a imagem no `OpenAiConsultantClient`
6. Busca na web (paridade com o Claude)
7. Frontend: seletor Claude/GPT + persistência em `localStorage` + rótulo do modelo na resposta
8. Testar os dois lado a lado com perguntas reais (inclusive a de "fornecedores em Limeira" que já usamos pra validar o Claude)

## 8. O que falta do usuário

- `OPENAI_API_KEY` (conta OpenAI com billing ativo)
- Confirmar o modelo exato a usar (ex: `gpt-4.1`, `gpt-4o` — depende de custo/
  qualidade desejados no momento da implementação, preços mudam)
