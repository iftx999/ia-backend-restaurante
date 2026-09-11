# Visão Geral do Projeto

> **Status:** Ideação / Planejamento
> **Última atualização:** 10/09/2026

## 1. Nome provisório
**RestorIA** (sugestão — pode trocar depois. Alternativas: GestorIA, RestGestor, PratoCerto)

## 2. Problema
Gerentes de restaurante lidam com dezenas de decisões operacionais e financeiras diariamente, mas raramente têm:
- Tempo para analisar dados de vendas, estoque e custos a fundo
- Conhecimento técnico de gestão financeira/operacional especializado no setor
- Uma ferramenta acessível (a maioria das soluções de BI para restaurantes são caras ou complexas demais para pequenos/médios negócios)

Resultado: dinheiro "vaza" por desperdício de insumos, precificação errada, ineficiência operacional e falta de compliance — sem que o gestor perceba a tempo.

## 3. Proposta de valor
Um assistente de IA especializado em gestão de restaurantes que:
1. **Tira dúvidas** sobre gestão, normas, finanças e operação (modo consultivo)
2. **Analisa dados reais** do restaurante (planilhas de vendas/estoque/custos) e aponta pontos de perda financeira e oportunidades de economia (modo analítico)
3. Fala a língua do gestor — sem jargão desnecessário, direto ao ponto, com recomendações acionáveis

## 4. Usuário-alvo (persona inicial)
- Gerente/dono de restaurante de pequeno a médio porte
- Não é especialista em gestão financeira nem em tecnologia
- Já usa (ou poderia usar) planilhas para controlar vendas/estoque
- Testador inicial: [conhecido do Matheus, gerente de restaurante] — validação real de hipóteses

## 5. Objetivos do projeto
| Objetivo | Tipo |
|---|---|
| Ajudar um gerente real a economizar/organizar melhor o restaurante | Objetivo de produto |
| Construir um case forte de portfólio (Java + Spring Boot + IA) | Objetivo de carreira |
| Aprender a integrar LLMs em aplicações backend reais | Objetivo técnico |

## 6. Escopo por fases (visão macro)
- **Fase 1 — Chatbot consultivo:** IA especializada responde perguntas de gestão, sem depender de dados do restaurante
- **Fase 2 — Módulo analítico:** Upload de planilhas → processamento → indicadores → relatório em linguagem natural
- **Fase 3 — Refinamento:** Histórico, alertas proativos, possível integração com PDV via API

Detalhes de cada fase em `01-requisitos-funcionais.md` e `04-roadmap.md`.

## 7. Fora de escopo (por enquanto)
- Integração direta com PDV/ERP do restaurante (fica pra fase futura, se houver PDV com API disponível)
- App mobile nativo (web responsivo é suficiente no MVP)
- Multi-restaurante / multi-tenant (foco inicial: 1 restaurante, 1 usuário)
- Automação de pedidos de compra ou ações financeiras automáticas (o sistema recomenda, não executa)

## 8. Métricas de sucesso do MVP
- O gerente real usa o chatbot pelo menos 1x/semana sem precisar de ajuda do Matheus
- Pelo menos 1 economia ou correção real identificada pelo módulo analítico nos primeiros 2 meses
- Sistema funcional documentado o suficiente para virar item de portfólio/entrevista
