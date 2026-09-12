# Arquitetura Técnica

## 1. Stack proposta

| Camada | Tecnologia | Observação |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.x | Alinhado com sua transição de carreira |
| Persistência | PostgreSQL | Já é sua stack conhecida |
| ORM | Spring Data JPA / Hibernate | Reforça o que você já vem estudando |
| Processamento de planilhas | Apache POI (XLSX) / OpenCSV (CSV) | Bibliotecas Java maduras |
| IA / LLM | API da Anthropic (Claude) | Via chamadas REST autenticadas por chave de API |
| Frontend | Angular | Reaproveita sua stack de frontend já usada em projetos anteriores |
| Autenticação | Spring Security + JWT | Simples, single-user no início, escalável depois |
| Build | Maven ou Gradle | Sugestão: Maven (mais comum em vagas) |
| Containerização | Docker | Facilita deploy e é bom ponto de portfólio |

## 2. Arquitetura em camadas (backend)

```
┌─────────────────────────────────────┐
│         Controller (REST API)         │  ← Endpoints: /chat, /analise, /relatorios
├─────────────────────────────────────┤
│              Service                   │  ← Regras de negócio, orquestra chamadas à IA
├─────────────────────────────────────┤
│         Repository (JPA)               │  ← Persistência (histórico, uploads processados)
├─────────────────────────────────────┤
│      Integration (Client Claude API)   │  ← Isolado em pacote próprio (ex: `integration.ai`)
└─────────────────────────────────────┘
```

Ponto importante: a chamada à API da Anthropic deve ficar **isolada numa camada de integração própria** (ex: `AiConsultantClient`), nunca espalhada pelo Service. Isso facilita trocar de provedor de IA no futuro e testar a aplicação sem depender da API externa (usando mocks).

## 3. Fluxo — Modo Consultivo (Fase 1)

```
Usuário → Frontend → POST /api/chat
                        ↓
                ChatController
                        ↓
                ChatService (monta contexto + histórico)
                        ↓
                AiConsultantClient (chama API Claude com system prompt especializado)
                        ↓
                Resposta salva no histórico (opcional) + retornada ao usuário
```

## 4. Fluxo — Modo Analítico (Fase 2)

```
Usuário → Upload planilha (vendas/estoque)
                        ↓
                UploadController → valida formato
                        ↓
                PlanilhaParserService (Apache POI/OpenCSV → extrai dados brutos)
                        ↓
                IndicadorCalculatorService (calcula CMV, margem, giro de estoque etc.)
                        ↓
                RelatorioService monta um payload estruturado (JSON com indicadores)
                        ↓
                AiConsultantClient (envia indicadores + pede análise em linguagem natural)
                        ↓
                Relatório salvo + retornado ao usuário
```

**Decisão de design importante:** os cálculos numéricos (CMV, margens, anomalias) são feitos em **Java puro**, não pela IA. A IA só recebe os números já processados e gera a interpretação/recomendação em linguagem natural. Isso evita erros de cálculo por parte do modelo e deixa a lógica de negócio testável.

## 5. Integração com a API da Anthropic

- Chamadas ao endpoint `/v1/messages` da API
- System prompt especializado definido em um arquivo de configuração/constante, versionado no repositório
- Chave de API armazenada em variável de ambiente, nunca hardcoded
- Prever tratamento de erro/timeout na chamada externa (a IA pode falhar ou demorar)

## 6. Estrutura de pacotes sugerida (backend)

```
com.restoria
├── chat
│   ├── ChatController
│   ├── ChatService
│   └── ChatMessage (entity)
├── analise
│   ├── UploadController
│   ├── PlanilhaParserService
│   ├── IndicadorCalculatorService
│   └── RelatorioService
├── integration.ai
│   └── AiConsultantClient
├── security
│   └── (config Spring Security/JWT)
└── shared
    └── (exceptions, utils, DTOs comuns)
```

## 7. Multi-provedor de IA (planejado)

A interface `AiConsultantClient` foi desenhada desde o início para permitir
trocar de provedor sem reescrever `ChatService`/`ConversaHistoricoService`.
Plano completo (roteamento Claude/GPT, config, streaming, testes) em
`docs/05-multi-provedor-ia.md` — bloqueado até `OPENAI_API_KEY` ser fornecida.

## 8. Decisões em aberto (a definir)
- [x] Maven ou Gradle? → **Maven** (com Maven Wrapper `./mvnw`)
- [ ] Deploy: onde vai rodar (VPS própria, Railway, Render, etc.)?
- [ ] Vai ter múltiplos usuários desde o início ou só 1 (o gerente teste)?
- [ ] Frontend: Angular direto ou começa com Thymeleaf/HTML simples pra validar rápido?
