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
- [ ] RF-13: validação de planilha implementada de forma básica (coluna ausente/vazia, número/data inválidos); falta cobertura de mais casos de planilha malformada

## Fase 3 — Refinamento
- [x] Histórico de relatórios e comparação mês a mês (RF-15) — backend: `GET /api/analise/relatorio` (histórico) e `GET /api/analise/relatorio/comparar` (`RelatorioComparador`, Java puro, com testes); frontend: seção "Comparar relatórios" em `AnaliseComponent`, selects com o histórico e tabela de variação de CMV/margem por prato
- [ ] Alertas proativos
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
- [ ] Testar checkout de ponta a ponta com Stripe em modo teste (precisa de conta
      Stripe real + `STRIPE_API_KEY`/`STRIPE_WEBHOOK_SECRET`/`STRIPE_PRICE_ID_PRO`)
- [ ] Migrar de `ddl-auto: update` para Flyway antes de ir pra produção com dado
      real de pagamento (deferido — ver nota no plano técnico da conversa)
- [ ] Multi-usuário por restaurante (papéis/equipes) — fora de escopo desta fase

## Multi-provedor de IA (Claude / GPT) — bloqueado até `OPENAI_API_KEY`
> Plano técnico completo em `docs/05-multi-provedor-ia.md`. Não iniciar antes
> da chave da OpenAI ser fornecida pelo usuário (pedido explícito em 2026-09-12).
- [ ] `ModeloIa` enum + `OpenAiProperties` + `OpenAiConsultantClient` + `MockOpenAiConsultantClient`
- [ ] `AiConsultantClientRouter` + `ChatRequest.modeloIa` + `ChatService` roteando por modelo
- [ ] Streaming (`OpenAiSseStreamProcessor`) e suporte a imagem no client OpenAI
- [ ] Busca na web no GPT (paridade com o que já existe no Claude)
- [ ] Frontend: seletor Claude/GPT (padrão Claude) + rótulo do modelo na resposta

## Marcos de portfólio
- [ ] Repositório público no GitHub com README bem documentado
- [ ] Deploy funcional (link ao vivo para mostrar em entrevistas)
- [ ] Post no LinkedIn contando a motivação e os aprendizados do projeto
- [ ] Testes automatizados nas regras de negócio (bom ponto para falar em entrevista técnica)
