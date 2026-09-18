# Login com Google (OAuth)

> Status: **Planejado, aguardando `GOOGLE_CLIENT_ID`**. Não implementar antes
> disso — ver `04-roadmap.md`. Frontend já está pronto visualmente (botão
> "Continuar com Google" na tela de login, desabilitado com badge "Em breve"
> — `login.component.html`); falta só ligar o fluxo de verdade.

## 1. Objetivo

Deixar o usuário entrar/criar conta com a conta Google, sem digitar senha.
Login e senha continuam existindo — Google é uma opção a mais, não substitui.

## 2. Como funciona (fluxo escolhido: ID token, sem client secret)

Usa **Google Identity Services (GIS)**, o SDK atual do Google pra "Sign in
with Google" (substituiu o antigo Google Sign-In JS). O fluxo de ID token é
mais simples que OAuth "Authorization Code" completo porque **não precisa de
client secret nem de redirect no backend** — só o Client ID (público) no
frontend:

1. Frontend carrega o script do GIS e renderiza o botão (ou dispara o fluxo
   programaticamente ao clicar em "Continuar com Google").
2. Usuário autentica com a conta Google; o GIS devolve um **ID token** (JWT)
   assinado pelo Google, direto no navegador — sem round-trip pelo backend.
3. Frontend envia esse ID token pro backend: `POST /api/auth/google { idToken }`.
4. Backend valida o token (assinatura + `aud` = nosso Client ID + `exp`) usando
   a biblioteca oficial `google-api-client` (verificação local via chaves
   públicas do Google, cacheadas — não precisa de round-trip síncrono pro
   Google a cada login).
5. Backend extrai `email`, `name` do token verificado, busca `Usuario` por
   email:
   - Existe → loga (emite JWT normal, mesmo formato de `AuthResponse`).
   - Não existe → cria `Usuario` novo (sem senha, ou senha aleatória
     inutilizável — ver §4) e entra direto no fluxo de onboarding já existente
     (nome/nome do restaurante), igual um cadastro por e-mail faria.

## 3. Design — Backend

### 3.1 Dependência

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version><!-- fixar a mais recente no momento da implementação --></version>
</dependency>
```
(traz `GoogleIdTokenVerifier`, que já cuida de cache das chaves públicas do
Google e validação de assinatura/expiração/audience.)

### 3.2 Endpoint novo (`com.restoria.security`)

```java
// AuthController — mesmo controller do login/registro por e-mail
@PostMapping("/google")
public AuthResponse loginComGoogle(@RequestBody @Valid GoogleLoginRequest request) {
    return authService.loginComGoogle(request.idToken());
}

public record GoogleLoginRequest(@NotBlank String idToken) {}
```

`AuthService.loginComGoogle(String idToken)`:
1. Verifica o token com `GoogleIdTokenVerifier` (configurado com o
   `GOOGLE_CLIENT_ID` como audience esperada).
2. Token inválido/expirado → `CredenciaisInvalidasException` (já existe,
   mesmo tratamento de senha errada hoje).
3. Token válido → busca/cria `Usuario` (ver §4), gera JWT com `JwtService`
   (já existe, mesmo usado no login por e-mail) e devolve `AuthResponse`
   igual ao login normal — **nenhuma mudança no contrato de resposta**, então
   o frontend trata a resposta do jeito que já trata hoje.

### 3.3 Configuração

```yaml
restoria:
  google:
    client-id: ${GOOGLE_CLIENT_ID:}
```

Novo `GoogleProperties` (`@ConfigurationProperties(prefix = "restoria.google")`),
mesmo padrão de `AiProperties`/`JwtProperties`. Chave nunca hardcoded (mesma
regra do `CLAUDE.md`).

## 4. Modelo de dados — usuário sem senha

`Usuario.senha` hoje é obrigatório (hash BCrypt). Duas opções:

- **A) Tornar `senha` opcional** (nulo pra contas Google) — mais correto
  semanticamente, mas precisa ajustar todo lugar que assume senha presente
  (login por e-mail deve recusar login por senha se `senha == null`, com
  mensagem clara "essa conta usa login com Google").
- B) Gerar uma senha aleatória inutilizável (usuário nunca vê, nunca loga por
  e-mail) — evita migração de schema, mas é um workaround menos limpo.

Recomendação: **A**, é pouco esforço extra (migração simples de coluna
nullable) e evita a inconsistência de "senha que existe mas não deveria
funcionar".

Novo campo também útil: `Usuario.origemCadastro` (Enum: `EMAIL`, `GOOGLE`) —
não é estritamente necessário pro login funcionar, mas ajuda a UI a decidir
se mostra campo de senha/opção de trocar senha no perfil.

## 5. Design — Frontend

- Script do GIS carregado sob demanda (não no `index.html` de cara — só
  quando a tela de login monta, evita custo em outras rotas):
  `https://accounts.google.com/gsi/client`.
- `google.accounts.id.initialize({ client_id, callback })` +
  `google.accounts.id.prompt()` (ou renderizar o botão oficial do Google via
  `renderButton`, que tem estilo próprio — decidir na hora se mantém o botão
  customizado atual, visualmente consistente com o app, ou troca pelo botão
  oficial do Google, mais "confiável" aos olhos do usuário mas foge do design
  system).
- `callback` recebe `{ credential }` (o ID token) → `AuthService.loginComGoogle(credential)`
  → `POST /api/auth/google` → mesmo tratamento de sucesso/erro que
  `AuthService.login` já tem hoje (salva token/usuário, navega pra `/`).
- Remover `googleIndisponivel = true` e o `disabled`/badge "Em breve" do botão
  em `login.component.ts`/`.html` quando isso for implementado.

## 6. Testes

- `AuthServiceTest` (backend): mock do `GoogleIdTokenVerifier`, cobrindo
  token válido com usuário novo, token válido com usuário existente, e token
  inválido/expirado.
- Teste manual: login com conta Google real em dev, conferir que cai no
  onboarding (nome/restaurante) igual um cadastro novo por e-mail cairia.

## 7. Ordem de implementação sugerida (quando a chave chegar)

1. Criar credencial OAuth no Google Cloud Console (tipo "Web application",
   com a origem do frontend nas "Authorized JavaScript origins")
2. `GoogleProperties` + dependência `google-api-client`
3. Migração: `Usuario.senha` nullable + `Usuario.origemCadastro`
4. `AuthService.loginComGoogle` + `POST /api/auth/google`
5. Frontend: carregar GIS, ligar o botão já existente, remover o estado
   "Em breve"
6. Testar de ponta a ponta com conta Google real

## 8. O que falta do usuário

- Criar um projeto no Google Cloud Console e gerar o **OAuth Client ID**
  (tipo "Web application") — não precisa de client secret pra esse fluxo
- Decidir a origem autorizada (domínio de produção quando houver deploy, e
  `http://localhost:4200`/`4300` pra dev)
