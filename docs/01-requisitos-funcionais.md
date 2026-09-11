# Requisitos Funcionais

> Ver `00-visao-geral.md` para contexto de negócio.

## Legenda de prioridade
- 🔴 Essencial (MVP não existe sem isso)
- 🟡 Importante (melhora muito, mas dá pra lançar sem)
- 🟢 Desejável (futuro)

---

## Fase 1 — Chatbot Consultivo

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | Usuário deve poder enviar perguntas em linguagem natural sobre gestão de restaurante | 🔴 |
| RF-02 | Sistema deve responder com base em um prompt de sistema especializado (CMV, ficha técnica, precificação, estoque, compliance sanitário, gestão de pessoas, custos operacionais) | 🔴 |
| RF-03 | Sistema deve manter histórico da conversa durante a sessão | 🔴 |
| RF-04 | Sistema deve persistir histórico de conversas entre sessões (usuário pode retomar depois) | 🟡 |
| RF-05 | Sistema deve ter autenticação simples (login/senha) — mesmo que single-user no início | 🟡 |
| RF-06 | Respostas devem citar categorias/temas (ex: "Isso é uma questão de Compliance Sanitário") para facilitar organização mental do gestor | 🟢 |

## Base de Conhecimento (RAG) — enriquecimento do modo consultivo

> Complementa a Fase 1: permite que o modo consultivo responda com base em uma
> base de boas práticas de gestão de restaurante (documentos próprios), além
> do conhecimento geral do modelo. Pode ser desenvolvido em paralelo às fases
> abaixo, já que é aditivo ao `ChatService` existente.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-19 | Sistema deve permitir ingerir documentos de texto (boas práticas, dicas, o que não fazer) em uma base de conhecimento pesquisável | 🟡 |
| RF-20 | Ao responder no modo consultivo, o sistema deve buscar trechos relevantes da base de conhecimento e usá-los como contexto adicional para a IA (RAG) | 🟡 |
| RF-21 | A IA deve basear-se no conteúdo recuperado da base quando disponível, sem inventar informações não presentes nela nem no que o usuário forneceu | 🟡 |

## Fase 2 — Módulo Analítico

| ID | Requisito | Prioridade |
|---|---|---|
| RF-07 | Usuário deve poder fazer upload de planilha (CSV/XLSX) com dados de vendas | 🔴 |
| RF-08 | Usuário deve poder fazer upload de planilha com dados de estoque/insumos | 🔴 |
| RF-09 | Sistema deve calcular indicadores básicos: CMV (Custo de Mercadoria Vendida), margem por prato, giro de estoque | 🔴 |
| RF-10 | Sistema deve identificar anomalias (ex: prato com margem negativa, insumo com alto índice de perda) | 🔴 |
| RF-11 | Sistema deve gerar um relatório em linguagem natural (via IA) resumindo os achados e recomendações | 🔴 |
| RF-12 | Sistema deve permitir que o usuário faça perguntas sobre o relatório gerado (ex: "por que esse prato está com margem baixa?") | 🟡 |
| RF-13 | Sistema deve validar formato/qualidade da planilha enviada e orientar o usuário em caso de erro | 🟡 |
| RF-14 | Sistema deve permitir exportar o relatório em PDF | 🟢 |

## Fase 3 — Refinamento

| ID | Requisito | Prioridade |
|---|---|---|
| RF-15 | Sistema deve guardar histórico de relatórios anteriores para comparação (ex: CMV mês a mês) | 🟡 |
| RF-16 | Sistema deve gerar alertas proativos quando um indicador sair da faixa esperada | 🟢 |
| RF-17 | Sistema deve permitir configurar metas/limites customizados por restaurante (ex: "CMV ideal = 30%") | 🟢 |
| RF-18 | Sistema deve suportar integração via API com sistemas de PDV, se disponível | 🟢 |

---

## Requisitos não-funcionais

| ID | Requisito |
|---|---|
| RNF-01 | Sistema deve responder ao chat em até ~5 segundos (dependente da API do modelo) |
| RNF-02 | Dados do restaurante (planilhas, relatórios) devem ser armazenados de forma segura e isolada por usuário |
| RNF-03 | Sistema deve ser acessível via navegador (responsivo, sem necessidade de instalação) |
| RNF-04 | Código deve seguir boas práticas de arquitetura em camadas (Controller/Service/Repository) para servir como case de portfólio |
| RNF-05 | Sistema deve ter testes automatizados nas regras de negócio críticas (cálculo de indicadores) |
