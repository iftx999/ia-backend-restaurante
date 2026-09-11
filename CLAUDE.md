# CLAUDE.md

Este arquivo é o contexto principal para o Claude Code trabalhar neste projeto. Documentação detalhada está em `docs/` — consulte quando precisar de mais profundidade sobre um tópico específico.

## O que é o projeto

Sistema de IA especializada em gestão de restaurantes ("RestorIA"). Dois modos:
1. **Consultivo** — chat que responde dúvidas de gestão de restaurante (CMV, estoque, precificação, compliance sanitário)
2. **Analítico** — usuário sobe planilha de vendas/estoque, sistema calcula indicadores e a IA gera um relatório apontando onde está saindo dinheiro

Contexto de negócio completo: `docs/00-visao-geral.md`
Requisitos detalhados por fase: `docs/01-requisitos-funcionais.md`

## Stack

- Java 21 + Spring Boot 3.x
- PostgreSQL + Spring Data JPA/Hibernate
- Apache POI (XLSX) / OpenCSV (CSV) para parsing de planilhas
- API da Anthropic (Claude) para geração de linguagem natural
- Angular (frontend)
- Spring Security + JWT
- Maven

Detalhes e diagramas de fluxo: `docs/02-arquitetura-tecnica.md`
Entidades e relacionamentos: `docs/03-modelo-dados.md`

## Regras de arquitetura (importante — seguir sempre)

- Arquitetura em camadas: `Controller → Service → Repository`
- Chamadas à API da Anthropic ficam **isoladas** no pacote `integration.ai` (ex: `AiConsultantClient`). Nunca chamar a API diretamente de um Service.
- **Cálculos numéricos (CMV, margem, indicadores) são feitos em Java puro**, nunca delegados à IA. A IA só recebe os números já calculados e gera a interpretação em linguagem natural. Isso é uma decisão de design deliberada — não mudar sem discutir.
- Chave de API da Anthropic sempre via variável de ambiente, nunca hardcoded.
- Estrutura de pacotes por feature (não por camada técnica global):
  ```
  com.restoria
  ├── chat
  ├── analise
  ├── assinatura
  ├── integration.ai
  ├── integration.billing
  ├── security
  └── shared
  ```

## Convenções de código

- Entidades e nomes de domínio em português (ex: `ItemVenda`, `nomePrato`), consistente com `docs/03-modelo-dados.md`
- DTOs separados de Entities — nunca expor entidade JPA diretamente no Controller
- Testes automatizados obrigatórios para regras de negócio de cálculo (indicadores, CMV, margem)
- Tratar erro/timeout em toda chamada à API externa da Anthropic

## Estado atual do projeto

Fase: **1 — Chatbot Consultivo** (esqueleto Spring Boot criado e subindo; sem lógica de negócio ainda)

Progresso e próximos passos: `docs/04-roadmap.md` (manter esse arquivo atualizado conforme tarefas forem concluídas)

## Ao trabalhar em uma tarefa

1. Verifique em qual fase do roadmap (`docs/04-roadmap.md`) a tarefa se encaixa
2. Confira se há requisito funcional específico em `docs/01-requisitos-funcionais.md` (IDs RF-XX)
3. Siga as regras de arquitetura acima sem exceção, mesmo que pareça mais rápido pular alguma
4. Ao concluir algo do roadmap, marque o checkbox correspondente em `docs/04-roadmap.md`
