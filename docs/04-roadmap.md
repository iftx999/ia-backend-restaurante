# Roadmap

## Fase 0 — Preparação (agora)
- [x] Estruturar documentação inicial (visão, requisitos, arquitetura, modelo de dados)
- [ ] Validar a ideia com o gerente do restaurante: quais são as 3 maiores dores dele hoje?
- [ ] Definir stack final (build: **Maven** definido; falta definir onde fazer deploy)
- [ ] Criar repositório no GitHub com estrutura inicial do projeto

## Fase 1 — Chatbot Consultivo (MVP funcional mais rápido)
- [x] Setup do projeto Spring Boot (estrutura de pacotes, config básica)
- [x] Endpoint `/api/chat` funcional, sem persistência ainda (só request/response)
- [x] Construir e testar o system prompt especializado em gestão de restaurantes
- [x] Adicionar persistência de histórico (`ConversaChat`, `MensagemChat`)
- [x] Frontend simples (mesmo que só uma tela de chat)
- [x] Endpoint `POST /api/chat/stream` (SSE) para resposta em streaming, token a token (backend; frontend Angular consumindo ainda pendente)
- [x] Autenticacao real login/senha + JWT (RF-05): `POST /api/auth/registrar` e `POST /api/auth/login` (`com.restoria.security`), `JwtAuthenticationFilter` protegendo todas as rotas exceto `/api/auth/**` e `/api/health`, historico de conversas agora associado ao usuario autenticado (removido `UsuarioPadraoService`/usuario-fantasma). Frontend Angular ainda precisa consumir esse contrato (tela de login/registro + envio do header `Authorization`).
- [x] Verificacao de e-mail no cadastro — `POST /api/auth/verificar-email` (token do link, publico) e `POST /api/auth/reenviar-verificacao` (autenticado); token UUID com validade de 24h; frontend: `/verificar-email` (rota nova, sem guard) e banner no chat pra confirmar/reenviar quando `emailVerificado=false`. Não bloqueia uso do chat (decisão de produto). Implementado em 2026-09-13.
- [x] Revisão de segurança (2026-09-13): rate limit de login por e-mail (`LoginRateLimiter`, 5 falhas/15min, em memória — não sobrevive a múltiplas instâncias, migrar pra store compartilhado tipo Redis se o deploy virar multi-instância) e cooldown de 60s no reenvio de verificação (`AuthService.reenviarVerificacao`). Pendências de menor prioridade não resolvidas: token JWT em localStorage (exposto a XSS, sem `innerHTML` hoje mas é risco estrutural do padrão SPA+JWT), senha mínima de 6 caracteres, e-mail já cadastrado é enumerável via `POST /api/auth/registrar`.
- [ ] Testar com o gerente real → coletar feedback

## Base de Conhecimento (RAG) — em paralelo, aditivo ao modo consultivo
- [x] Estrutura: entidades `DocumentoConhecimento`/`TrechoConhecimento`, extensão `pgvector`, `EmbeddingClient` (Voyage AI) isolado em `integration.ai`, `ConhecimentoService` (ingestão + busca por similaridade)
- [x] Integrar busca de trechos relevantes ao `ChatService` (RAG no modo consultivo)
- [x] Ingerir conteúdo real de boas práticas de gestão de restaurante (5 documentos: CMV, precificação/ficha técnica, controle de estoque, compliance sanitário, gestão de pessoas — `src/main/resources/conhecimento-seed/`)
- [ ] Testar qualidade das respostas com a base carregada vs. sem ela (precisa de `ANTHROPIC_API_KEY` real — não dá pra avaliar qualidade com o modo mock)

## Fase 2 — Módulo Analítico
- [x] Definir template de planilha: colunas aceitas (com aliases tolerantes) documentadas em `VendasPlanilhaMapeador`/`EstoquePlanilhaMapeador` — vendas: `prato`, `quantidade`, `preco`, `custo` (opcional), `data` (opcional); estoque: `insumo`, `quantidade`, `custo`, `perda` (opcional), `data` (opcional)
- [x] Implementar upload + parsing (Apache POI / OpenCSV) — `POST /api/analise/upload/vendas` e `POST /api/analise/upload/estoque`, pacote `com.restoria.analise`
- [x] Implementar cálculo de indicadores (CMV global, margem por prato, percentual de perda por insumo) — `IndicadorCalculator`, Java puro, com testes (`IndicadorCalculatorTest`). "Giro de estoque" clássico (custo consumido / estoque médio) não é calculável sem ficha técnica prato→insumo nem saldo de estoque; ver limitação documentada em `IndicadorCalculator` e pergunta em aberto em `docs/03-modelo-dados.md`
- [x] Implementar detecção de anomalias (RF-10): margem de prato abaixo de 20% (configurável, `IndicadorCalculator.MARGEM_MINIMA_ESPERADA`) e perda de insumo acima de 5% (`PERCENTUAL_PERDA_ALERTA`)
- [x] Implementar geração de relatório via IA a partir dos indicadores — `POST /api/analise/relatorio`, `AnaliseSystemPrompt`, reaproveita `AiConsultantClient` existente (RF-11)
- [x] RF-12: permitir perguntas sobre o relatório gerado — `POST /api/analise/relatorio/{id}/perguntar`, pergunta pontual (sem histórico persistido), IA recebe o relatório já gerado como contexto
- [x] Frontend: tela de upload/relatório (`AnaliseComponent`, rota `/analise`) — upload de vendas/estoque, geração e exibição do relatório, pergunta sobre o relatório, download de PDF/Excel; link de navegação adicionado na sidebar do chat
- [ ] Testar com dados reais (ou simulados) do restaurante
- [ ] Ajustar com base no feedback do gerente
- [x] RF-13: validação de planilha cobre coluna ausente/vazia, número/data inválidos, valores negativos (quantidade/preço/custo/perda), arquivo vazio/formato não suportado, linhas em branco no meio (CSV e XLSX tratados de forma consistente) e cabeçalho desconhecido — testes em `PlanilhaLeitorTest`/`PlanilhaColunaUtilTest`/`*PlanilhaMapeadorTest`. Não coberto: encoding de CSV fora de UTF-8 (planilhas exportadas do Excel BR às vezes vêm em Latin-1/Windows-1252 e quebram acentuação silenciosamente) e tratamento explícito de fórmulas XLSX.

## Fase 3 — Refinamento
- [x] Histórico de relatórios e comparação mês a mês (RF-15) — backend: `GET /api/analise/relatorio` (histórico) e `GET /api/analise/relatorio/comparar` (`RelatorioComparador`, Java puro, com testes); frontend: seção "Comparar relatórios" em `AnaliseComponent`, selects com o histórico e tabela de variação de CMV/margem por prato
- [x] Alertas proativos (RF-16) — banner no chat quando o relatorio mais
      recente do usuario tem indicador fora da faixa esperada, ver
      `Relatorio.temAlerta`/`GET /api/analise/relatorio/alerta`
- [x] Exportação em PDF (RF-14) — `GET /api/analise/relatorio/{id}/pdf` (`RelatorioPdfExporter`, Apache PDFBox); adiantado da Fase 3 a pedido do usuário
- [x] Exportação em Excel (não estava nos requisitos originais, pedido explícito do usuário) — `GET /api/analise/relatorio/{id}/excel` (`RelatorioExcelExporter`, Apache POI)
- [ ] Avaliar integração com PDV, se o restaurante tiver um sistema com API

## Fase 4 — Conversão para SaaS (billing + limite de uso)
- [x] Modelo de plano/assinatura (`com.restoria.assinatura`: `Plano` enum GRATIS/PRO,
      `Assinatura` 1:1 com `Usuario`, criada automaticamente no registro)
- [x] `LimiteUsoService` — bloqueia mensagem de chat/geração de relatório quando o
      usuário estoura a cota mensal do plano (baseado em contagem por timestamp,
      sem contador próprio); HTTP 402 mapeado no `GlobalExceptionHandler`
- [x] Integração Stripe (`stripe-java`) — `POST /api/assinatura/checkout` (Checkout
      Session), `POST /api/assinatura/webhook` (sincroniza status via
      `checkout.session.completed`/`customer.subscription.updated`/`.deleted`),
      `GET /api/assinatura` (plano/status/uso atual)
- [x] Frontend: tela `/planos`, CTA de upgrade quando a API retorna 402
- [x] Cobertura de teste automatizado pro fluxo de billing (antes não tinha
      nenhum teste) — `LimiteUsoServiceTest` (bloqueio por cota mensal, plano
      PRO com limites maiores, assinatura INADIMPLENTE caindo pros limites do
      plano grátis) e `AssinaturaServiceTest` (`processarWebhook` testado de
      ponta a ponta com payloads assinados manualmente via HMAC — verificação
      de assinatura do Stripe é local, não precisa de rede — cobrindo os 3
      tipos de evento tratados, assinatura inválida, `Assinatura` inexistente,
      evento sem tratamento e `api_version` incompatível)
- [ ] Testar checkout de ponta a ponta com Stripe em modo teste (precisa de conta
      Stripe real + `STRIPE_API_KEY`/`STRIPE_WEBHOOK_SECRET`/`STRIPE_PRICE_ID_PRO`
      — isso ainda não foi feito; a cobertura de teste acima reduz o risco mas
      não substitui um teste real do checkout ponta a ponta)
- [x] Migrar de `ddl-auto: update` para Flyway — `V1__baseline_schema.sql`
      gerado a partir do schema real (pg_dump no banco de dev), `ddl-auto`
      agora em `validate`; `baseline-on-migrate`/`baseline-version: 1` fazem
      bancos existentes (dev local, deploys anteriores) serem marcados na V1
      sem recriar tabelas, enquanto bancos novos rodam a V1 normalmente.
      Testado de ponta a ponta contra o Postgres local (baseline aplicado,
      app subiu, `/api/health` respondeu OK). Testes continuam no H2 com
      `ddl-auto: create-drop` e Flyway desabilitado (H2 não suporta os tipos
      `vector`/`oid` usados no schema real).
- [ ] Multi-usuário por restaurante (papéis/equipes) — fora de escopo desta fase

## Multi-provedor de IA (Claude / GPT)
> Plano técnico completo em `docs/05-multi-provedor-ia.md`. `OPENAI_API_KEY`
> fornecida pelo usuário em 2026-09-18 — implementado no mesmo dia. Modelo
> configurado: `gpt-5.6-terra` (posicionamento equilibrado, confirmado com o
> usuário — nomes de modelo da OpenAI pesquisados via web, fora do
> conhecimento confiável do modelo no momento da implementação).
- [x] `ModeloIa` enum + `OpenAiProperties` + `OpenAiConsultantClient` + `MockOpenAiConsultantClient`
- [x] `AiConsultantClientRouter` + `ChatRequest.modeloIa` + `ChatService` roteando por modelo
      (Claude `@Primary`, GPT `@Qualifier("gpt")` — evita ambiguidade de bean)
- [x] Streaming (`OpenAiSseStreamProcessor`) e suporte a imagem no client OpenAI
- [x] Busca na web no GPT (paridade com o que já existe no Claude) — `tools: [{"type": "web_search"}]`
- [x] Frontend: seletor Claude/GPT (padrão Claude, persistido em `localStorage`) + rótulo do modelo na resposta
- [ ] Testar os dois lado a lado com perguntas reais (mock testado; chamada real à API da OpenAI ainda não validada — precisa rodar sem `restoria.ai.mock=true`)

## Geração/edição de imagem de prato (OpenAI)
> Plano técnico completo em `docs/06-geracao-imagem-ia.md`. Feature anunciada
> na landing page ("fotos com IA"). `OPENAI_API_KEY` fornecida em 2026-09-18;
> decisão de negócio (§6 do doc) confirmada com o usuário no mesmo dia:
> geração de imagem é **exclusiva do plano PRO** (20 imagens/mês, GRATIS não
> inclui) — implementado no mesmo dia.
- [x] `ImagemIaClient` + `OpenAiImagemClient` (`/v1/images/generations`, modelo `gpt-image-1`) + `MockImagemIaClient`
- [x] Storage local da imagem gerada + entidade `ImagemPrato` (migration V3)
- [x] `ImagemPratoService`/`ImagemPratoController` (`POST /api/imagens/gerar`)
- [x] Edição de imagem existente (`POST /api/imagens/editar`, usa a imagem já anexada no chat)
- [x] `LimiteUsoService.verificarLimiteImagem` + campo `imagensPorMes` no `Plano` (GRATIS=0, PRO=20)
- [x] Frontend: botão "Gerar imagem" no chat, bolha de resposta com imagem, download
- [ ] Testar com prompts reais (mock testado de ponta a ponta; chamada real à
      API de imagens da OpenAI ainda não validada — precisa rodar sem
      `restoria.ai.mock=true` e custa crédito de verdade por chamada)

## Login com Google (OAuth) — bloqueado até `GOOGLE_CLIENT_ID`
> Plano técnico completo em `docs/07-login-google-oauth.md`. Tela de login já
> redesenhada (split-screen, tema escuro) com o botão "Continuar com Google"
> no lugar, desabilitado com badge "Em breve" (`login.component.html`/`.ts`).
> Não iniciar antes do Client ID ser fornecido pelo usuário (pedido explícito
> em 2026-09-13).
- [ ] Criar credencial OAuth "Web application" no Google Cloud Console
- [ ] `GoogleProperties` + dependência `google-api-client`
- [ ] Migração: `Usuario.senha` nullable + `Usuario.origemCadastro`
- [ ] `AuthService.loginComGoogle` + `POST /api/auth/google`
- [ ] Frontend: carregar Google Identity Services e ligar o botão já existente

## Divulgação pública (grupos de Facebook, LinkedIn etc.)
> **Decisão do usuário (2026-09-13): só divulgar publicamente depois que a
> `OPENAI_API_KEY` chegar e a geração de fotos de prato (`docs/06-geracao-imagem-ia.md`)
> estiver no ar.** Fotos com IA é o diferencial mais forte anunciado na landing
> page — lançar sem essa feature funcionando geraria expectativa que o produto
> ainda não entrega.
- [ ] `OPENAI_API_KEY` já chegou (2026-09-18) — falta implementar `docs/06-geracao-imagem-ia.md`
- [ ] Só então: postar em grupos de Facebook de donos de restaurante e LinkedIn

## Marcos de portfólio
- [ ] Repositório público no GitHub com README bem documentado
- [ ] Deploy funcional (link ao vivo para mostrar em entrevistas)
- [ ] Post no LinkedIn contando a motivação e os aprendizados do projeto
- [ ] Testes automatizados nas regras de negócio (bom ponto para falar em entrevista técnica)
