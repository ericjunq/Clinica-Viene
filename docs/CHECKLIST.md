# ClinicaViene — Checklist

> Atualizar a cada alteração. Legenda: `[ ]` pendente · `[~]` em andamento · `[x]` concluído
> Item concluído sem testes executados: marcar `[~]` e anotar **"testes pendentes de execução"**.
> Desenvolvimento direto na `main` (D-058). Itens antigos citam a branch em que foram feitos.

## Pendências em aberto (resumo — atualizado em 2026-11-27)
Itens que ficaram para depois, com o motivo. O detalhe fica na fase de origem.

**Dependem do autor**
- [ ] Revisão da ficha de saúde (dentista) e dos termos (advogado) — arquivo enviado: `Revisao-ficha-de-saude-e-termos` (PDF/DOCX) (D-089)
- [ ] Dados da clínica para os termos: razão social, CNPJ, endereço, encarregado de dados (DPO), prazo de guarda do prontuário (D-040)
- [ ] Ligar a ZapSign ao fluxo da ficha e testar com documento real — quando o projeto estiver avançado (plano gratuito: 3 documentos/mês) (D-103)
- [ ] Logo oficial em SVG/PNG para substituir o monograma desenhado (Q-012, D-082)
- [ ] Decisões A CONFIRMAR das próximas fases: hospedagem e backups (D-041), storage de produção (D-060), retenção (D-040), regras financeiras (D-012, D-015, D-035, D-036, D-038), tablet da recepção (D-037), impactos multiprofissionais (D-070)

**Técnicas**
- [ ] Formatar o backend (Spotless/palantir) e ligar `spotless:check` no CI (D-063) — pode ser feito pelo próprio CI
- [ ] Fonte completa no PDF da ficha (hoje caracteres fora do português, como emoji, saem como `#`) — só se fizer falta (D-102)
- [ ] E2E do botão "Abrir neste aparelho" (encerra a sessão da recepção e abre o quiosque) — o E2E atual abre o link do convite direto (D-104)

## Fase 0 — Fundação do repositório
Feita inicialmente em branches empilhadas (D-054), integradas na `main` por fast-forward em 2026-11-26. Daqui em diante, tudo direto na `main` (D-058).

- [x] Gerar projeto no Spring Initializr e subir para o GitHub (commit inicial na `main`)
- [x] Adicionar `CLAUDE.md`, `docs/`, `.claude/settings.json` ao repositório — branch `docs-fundacao-projeto`
- [x] Registrar decisões da Fase 0 (D-043, D-044, D-046, D-047, D-049 a D-055) e criar `docs/DUVIDAS.md` — branch `docs-fundacao-projeto`
- [x] Mover projeto Spring para `backend/` + `.gitignore` geral na raiz — branch `backend-estrutura-monorepo`
- [x] Spring Modulith 2.2.0 GA; remover `thymeleaf-extras-springsecurity6` (D-049, D-051) — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
- [x] Dependências manuais no `pom.xml` (Actuator, springdoc, MapStruct, openhtmltopdf, ZXing, AWS SDK S3, Bucket4j, REST Assured, ArchUnit, GreenMail) — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
  - Testcontainers MinIO **não** adicionado: imagens oficiais removidas (D-056). Storage será testado com SeaweedFS via `GenericContainer`.
- [x] Lombok: `lombok.config`, ordem dos annotation processors, teste ArchUnit das regras (`EntidadesLombokArchTest`) (D-051) — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
- [x] Plugins: JaCoCo, Spotless, Surefire (`*Test`) / Failsafe (`*IT`) — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
  - [ ] Rodar `./mvnw spotless:apply` em máquina com Java, commitar `style: aplica formatação palantir` e ligar `spotless:check` no CI no mesmo push (D-063)
  - [ ] Metas de cobertura JaCoCo por módulo quando os módulos existirem
- [x] Flyway: `ddl-auto=validate`, migration `btree_gist`, tabela `event_publication` do Modulith; `db/testdata` só no perfil de teste (`MigracoesIT`) — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
- [x] ~~Teste de módulos (`ModulosTest` — `ApplicationModules.verify()`)~~ — removido pela D-078 — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
- [x] `compose.yaml` na raiz (Postgres 18.7-alpine, Mailpit, backend no profile `app`), `.env.example`, Dockerfile multi-stage do backend (D-050) — branch `docker-compose-base` — **CI verde**: compose validado e imagem construída (2026-11-26)
  - [ ] Validar localmente (máquina com Docker): `cp .env.example .env && docker compose up -d`, rodar o backend pela IDE, e `docker compose --profile app up -d --build`
- [~] Storage S3 local no compose: SeaweedFS `4.55` (`server -s3`, porta 8333 só em localhost) — D-056 — compose validado no CI; não executado localmente
- [x] CI: testes do backend (`./mvnw verify`) + relatórios de teste e cobertura; validação do `compose.yaml` e build da imagem do backend (sem publicar) — a cada push e em PR para `main` (D-046) — branch `ci-pipeline-inicial` — **CI verde**: 8 testes unitários + 3 de integração passaram (2026-11-26)
- [ ] CI: lint do backend (Spotless — D-063), metas de cobertura, scan de dependências (conforme existirem)
- [x] Gerar frontend Angular 22.3 (`frontend/`, standalone, zoneless, SCSS, Vitest, sem SSR) + casca mínima — branch `frontend-projeto-angular` — testes unitários **executados localmente: 2 passaram** (`npm test -- --watch=false`) e `npm run build` ok (2026-11-26)
- [x] Dockerfile multi-stage do frontend (Node 24 → nginx sem root, proxy `/api/` → backend) + serviço `frontend` no compose (profile `app`) + proxy do `ng serve` — branch `frontend-projeto-angular` — **CI verde**: imagem construída (2026-11-26)
- [x] CI: job `frontend` (npm ci, Vitest, build) e build da imagem do frontend — branch `frontend-projeto-angular` — **CI verde** (2026-11-26)
- [x] Atualizar actions do CI para versões com Node 24 (checkout v7, setup-java v6, setup-node v7, upload-artifact v7) — branch `ci-atualiza-actions` — **CI verde**, sem avisos de depreciação (2026-11-26)
- [x] Lint do frontend (angular-eslint 22.8.0) + Prettier (`npm run lint`, `npm run format:check`) no job `frontend` do CI — branch `frontend-lint` — **executados localmente: lint ok, formatação ok, 2 testes passaram** (2026-11-26)
- [ ] Playwright (E2E) — configurar junto com a primeira tela real (login, Fase 1)
- [x] Identidade visual Viene (cores, Cormorant Garamond + Montserrat hospedadas localmente, logo, fundos) (D-082)
- [x] Licença do PrimeNG: trocado por **Angular Material** (MIT) — D-084; telas refeitas mantendo o visual (32 testes unitários passaram localmente)
- [ ] Logo oficial em SVG/PNG para substituir o monograma desenhado (D-082)
- [ ] **Tarefa manual do autor**: ruleset na `main` no GitHub bloqueando force push e exclusão (sem exigir PR — desenvolvimento direto na `main`, D-058/D-065)
- [x] Política de migrations registrada (D-047) — branch `docs-fundacao-projeto`
- [~] Renomear pacote para `br.com.clinicaviene` (groupId, artifactId `clinica-viene`, fixtures em `fixtures.lombok`) — D-062
- [~] Eventos do Modulith: `completion-mode=archive`, republicação ao reiniciar, `event_publication_archive` via Flyway (V3), init automático desligado — D-059
- [ ] Limpeza periódica de `event_publication_archive` (job agendado; definir prazo de retenção junto da D-040)
- [x] Pasta `docker/` dispensada: Dockerfiles em `backend/` e `frontend/`, compose na raiz (D-064)
- [ ] Decidir forma de carregar seeds de configuração real (D-048 — A CONFIRMAR)

- [x] README do projeto (raiz) e do frontend: visão geral, pré-requisitos, clonar, `.env`, como rodar, portas, testes, estrutura, segurança

## Fase 1 — Identidade, auditoria e login
Plano aprovado em 2026-11-26 (D-048, D-066 a D-076).

**1A. Base compartilhada**
- [x] Criptografia: `CampoCifradoConverter` + `Cifrador` (AES-256-GCM, chave versionada na AAD), `HashCpf` (HMAC-SHA256), validador `Cpf`; app não sobe sem as chaves; `.env` da raiz lido em dev — **CI verde** (26 testes unitários)
- [x] Problem Details (RFC 9457, inclusive 401/403 do Spring Security) + OpenAPI em `/api/v1` (desligado por padrão; `OPENAPI_HABILITADO=true` em dev) — **CI verde**
- [x] Spring Security base: tudo fechado por padrão, stateless, `@EnableMethodSecurity` (`SegurancaBaseIT`) — **CI verde**

**1B. Auditoria**
- [x] `audit_log` (V4) + trigger que rejeita `UPDATE`/`DELETE`/`TRUNCATE` (D-076) + `ServicoAuditoria` (`registrar` na transação da operação, `registrarIndependente` em transação própria) + IP real atrás do proxy (`AuditoriaIT`) — **CI verde**

**1C. Usuários**
- [x] `Usuario` (V5: CPF cifrado + `cpf_hash`, perfil, status ATIVO/INATIVO, área de atuação + registro profissional único, trocarSenha; CHECKs no banco) — D-061, D-068, D-069
- [x] Regras: DEV/MASTER criam ADMIN/PROFISSIONAL; DEV cria MASTER; MASTER cria MASTER; nenhum endpoint cria DEV; MASTER não gerencia DEV; DEV não desativa MASTER; ninguém altera o próprio status (D-067) — `RegrasDePerfil` + `ServicoUsuarios`; endpoints `/api/v1/usuarios` (testados via HTTP na 1D)
- [x] Política de senha (D-066) + Argon2id (D-074) + troca da própria senha — **CI verde**
- [x] Inicializador do DEV (D-048) — `DEV_INICIAL_*` — **CI verde**
- [x] Edição de dados do usuário e mudança de perfil (D-086): `PUT /usuarios/{id}`, `PATCH /usuarios/{id}/perfil`, tela com "Editar"; perfil passa a vir do banco a cada requisição — **CI verde**

**1D. Autenticação**
- [x] Sessões: JWT RS256 15 min (`JWT_CHAVE_PRIVADA`), token de renovação opaco (hash SHA-256) com rotação, fim absoluto de 12 h, reuso → revoga a cadeia (tolerância de 20 s para corrida entre abas → 409), logout (`ServicoSessoesIT`)
- [x] Login em 2 passos: `LoginService` (desafio), `DoisFatoresService`, `AuthController` (`/login`, `/2fa/cadastro`, `/2fa/confirmar`, `/2fa/verificar`, `/renovar`, `/logout`, `/eu`) + cookie `cv_renovacao` (D-071, D-079) — `AuthControllerIT` — **CI verde**
- [x] Status checado a cada requisição (`UsuarioJwtConverter`); desativar revoga sessões (D-068); troca de senha pendente → só rotas de troca de senha
- [x] Rate limit de login (Bucket4j: 20/IP, 5 falhas/CPF em 15 min → 429 + `Retry-After`) (D-079)
- [x] Limpeza periódica dos buckets ociosos do rate limit (a cada 15 min; Bucket4j com o relógio da aplicação) (D-087) — **CI verde**

**1E. 2FA**
- [x] TOTP (RFC 6238, janela ±1, anti-replay) + QR code (ZXing) + 10 códigos de recuperação (D-072, D-079)
- [x] Código 2FA por e-mail (`POST /auth/2fa/email`, método `EMAIL`) com regras de reenvio genéricas em `EnvioCodigoService` (espera 1/2/5/10 min, 5 envios, depois 1 h; 10 min de validade; 5 erros) (D-072, D-073) — `EnvioCodigoServiceIT`, `AuthControllerIT` — **CI verde**

**1F. Frontend**
- [x] Telas (hoje em Angular Material — D-084) de login, 2FA (cadastro com QR, códigos de recuperação, verificação por app/e-mail/recuperação com contagem para reenvio), troca de senha, recuperação de senha, usuários (lista, cadastro, ativar/desativar) (D-081) — **testes unitários executados localmente: 31 passaram**; lint e build ok
- [x] Interceptor de renovação automática + guards por sessão, troca de senha e perfil (D-081) — testados (Vitest)
- [x] Playwright + job E2E no CI (compose completo, `.env` descartável): primeiro acesso do DEV, senha errada, rota protegida (D-081) — **CI verde (3 testes no navegador)**
- [x] Build da imagem do backend com a imagem oficial `maven:3.10.2-eclipse-temurin-25-alpine` (sem baixar o Maven a cada build) (D-087) — **CI verde**

**1G. Recuperação de senha por e-mail** (opcional)
- [x] Recuperação de senha: `POST /auth/senha/recuperar` (resposta sempre igual) → código de 6 dígitos por e-mail → `POST /auth/senha/redefinir`; senha validada antes de gastar o código; derruba as sessões; 2FA continua exigido no login (D-073, D-080) — `AuthControllerIT` — **CI verde**; tela no frontend

- [x] Reorganizar backend em camadas na raiz (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/`, `exceptions/`…) com nomes `XxxService`/`XxxController`/`XxxRepository` (D-078; substitui a D-077); `ModulosTest` removido — **CI verde**
- [x] `@RequiredArgsConstructor` nos services, controllers e componentes (D-051)

- [ ] Avaliar regras ArchUnit de acesso a dados de saúde (só o service do assunto usa o repository) — D-078, na Fase 2

**Testes da fase**
- [x] Matriz de autorização (cada perfil × cada endpoint) — `UsuarioControllerIT` para `/api/v1/usuarios` — **CI verde** (repetir para cada endpoint novo)
- [x] Cobertura mínima no build: 85% total, 80% em `service` e `auth` (D-087) — **CI verde**

## Fase 2 — Pacientes e anamnese
Plano aprovado em 2026-11-27 (D-088 a D-095).

**2A. Cadastro do paciente**
- [x] `IdUtils.novoId()` (UUID v7) como gerador padrão de ids, aplicado às entidades (D-095) — **CI verde**
- [x] `Paciente` (CPF cifrado + `cpf_hash` único parcial), `ResponsavelLegal` (CPF cifrado obrigatório), `PacienteVersao` (histórico cifrado, somente inclusão no banco) — V11 — **CI verde**
- [x] Regras de CPF (D-053): menor sem CPF → responsável com CPF; adulto sem CPF só com aprovação do MASTER + justificativa + auditoria; alerta de duplicidade (409 + confirmação) — **CI verde**
- [x] Pré-cadastro pela recepção; busca por CPF/nome/nascimento; edição de endereço/telefone/e-mail com versão auditada; nome/CPF/nascimento não editáveis (D-092) — `/api/v1/pacientes` — **CI verde**
- [x] Leitura de ficha cadastral auditada; matriz de autorização (ADMIN/MASTER sim; PROFISSIONAL/DEV não) — `PacienteServiceIT`, `PacienteControllerIT` — **CI verde**

**2B. Termos LGPD**
- [x] `TermoVersao` + `AceiteTermo` (D-075) + consentimentos opcionais revogáveis (D-093) + textos-exemplo (D-096) — backend (V12) — **CI verde**; tela do MASTER na 2G

**2C. Modelo de anamnese**
- [x] `ModeloAnamnese` versionado por área (tipos de pergunta, perguntas de alerta clínico, validação de respostas), exemplo odontológico completo (46 perguntas) (D-089, D-096) — backend (V13) — **CI verde**; tela do MASTER na 2G

**2D. Convite**
- [x] `ConviteCadastro` (token hash, uso único, um ativo por paciente; LINK 72 h com QR/e-mail e reenvio D-073; QUIOSQUE 2 h com documento conferido) (D-098) — backend (V14) — `ConviteServiceIT`, `ConviteControllerIT` — **CI verde**; telas na 2G

**2E. Fluxo público do paciente**
- [x] Backend: abrir convite, código no e-mail (LINK) ou sessão direta (QUIOSQUE), sessão de preenchimento de 2 h, formulário (pré-cadastro + anamnese vigente + termos vigentes), revisão completa da ficha (D-091, D-100) — V15 — `CadastroPublicoServiceIT`, `CadastroPublicoControllerIT` — **CI verde**
- [x] Frontend: tela pública `/cadastro#<token>` (código ou quiosque, seus dados, ficha de saúde, privacidade, conferir) — Vitest — **CI verde**
- [x] E2E do fluxo do paciente (Playwright) — junto da 2G (`cadastro-paciente.spec.ts`) — **CI verde**

- [x] Área da ficha de saúde escolhida no convite (D-101, Q-013) — coluna no CREATE da V14 — **CI verde**

**2F. Assinatura e documento**
- [x] `ProvedorAssinatura` + provedor interno (desenho/nome digitado + evidências) — `ProvedorAssinaturaInternoTest` — **CI verde**
- [x] Estrutura do adaptador ZapSign (`ZapSignClient`, desligado por padrão, limite de 10 MB) (D-103) — `ZapSignClientTest` — **CI verde**
- [ ] Ligar a ZapSign ao fluxo da ficha e testar com documento real (quando o projeto estiver avançado — plano gratuito: 3 documentos/mês) (D-090, D-103)
- [x] PDF (Thymeleaf → openhtmltopdf) + SHA-256 + storage S3 privado cifrado pela aplicação (SeaweedFS em dev/teste, contêiner nos testes — D-056, D-102) — `ArmazenamentoServiceIT`, `CifradorAesGcmTest` — **CI verde**
- [x] `FichaAnamnese`, `Documento`, `Assinatura` imutáveis (V16, banco rejeita UPDATE/DELETE), respostas e evidências cifradas; ao assinar grava dados conferidos (versão), decisões dos termos e encerra o convite (D-088, D-102) — `FichaAssinaturaServiceIT`, `CadastroPublicoControllerIT` — **CI verde**
- [ ] Leitura da ficha pelo profissional vinculado (Fase 4) (D-088)
- [x] Entrega do PDF por link + CPF (backend: e-mail com link `/documento#<token>`, 7 dias, 5 downloads, 5 CPFs errados bloqueiam; confere o hash ao baixar) (D-094) — **CI verde**
- [x] Frontend: passo de assinatura (desenho no quadro ou nome digitado + declaração) e página `/documento` de download com CPF — Vitest — **CI verde**

**2G. Telas internas**
- [x] Backend: documentos do paciente (só metadados) e novo link de entrega gerado pela recepção (QR na tela ou e-mail) (D-104) — `DocumentoControllerIT` — **CI verde**
- [x] Pacientes: lista/busca, pré-cadastro (menor, adulto sem CPF, duplicidade), ficha com cadastro/contato, convite (link/QR, quiosque, cancelar), documentos (QR de download), privacidade (revogar), histórico (D-104) — Vitest + E2E — **CI verde**
- [x] Configurações do MASTER: termos (exemplos, rascunho, publicar, nova versão) e fichas de saúde com editor de perguntas (D-104) — Vitest + E2E — **CI verde**

- [x] CI: cache de dependências na montagem das imagens (camada `dependency:go-offline` + cache do GitHub Actions) contra 429 do Maven Central (D-097) — **CI verde**; job Docker de 3m25s para 23s com cache

**Testes da fase**
- [x] Regras ArchUnit de camadas: só o `FichaAnamneseService` usa o `FichaAnamneseRepository`; controllers não acessam repositories (D-078, D-105) — `CamadasArchTest` — **CI verde**
- [x] E2E: pré-cadastro → quiosque → anamnese → termos → assinatura → ficha concluída → download com CPF (D-104) — **CI verde**

**Pendências que dependem do autor**
- [x] Conta ZapSign criada (plano gratuito, 3 documentos/mês) — teste real adiado (D-103)
- [ ] Revisão do exemplo de anamnese (dentista) e dos termos (advogado) antes de publicar (D-089) — arquivo de revisão gerado em 2026-11-27, aguardando retorno
- [ ] Ligar a ZapSign e testar com documento real (D-103)

## Fase 3 — Agenda
- [x] `Consultorio` e `CategoriaProcedimento` (por área, código estável, exemplo de odontologia) — MASTER (D-106) — V17 — `AgendaConfiguracaoControllerIT`, `CategoriaProcedimentoServiceTest` — implementado — testes pendentes de execução
- [x] `DisponibilidadeProfissional` (fixos semanais) + `ExcecaoAgenda` (horário extra / ausência) + cálculo de disponibilidade, grade de 15 min (D-106) — `DisponibilidadeCalculoTest`, `AgendaConfiguracaoControllerIT` — implementado — testes pendentes de execução
- [ ] `BloqueioAgenda` (bloqueio de consultório sem paciente)
- [ ] `Agendamento` com itens (categoria + descrição), status completo, exclusão de conflitos no banco e recusa fora da disponibilidade (D-106); aviso de ficha pendente
- [ ] Marcar / remarcar / cancelar (com motivo)
- [ ] Check-in + notificação WebSocket ao profissional
- [ ] Calendário próprio (D-085): visão do dia com uma coluna por consultório/profissional (grade de horários, agendamentos, bloqueios, indisponibilidade esmaecida, cor por status); clicar em horário livre → novo agendamento
- [ ] Calendário próprio: semana de um profissional
- [ ] Calendário próprio: lista do dia para o check-in
- [ ] Calendário próprio: arrastar para remarcar (CDK Drag & Drop, com confirmação e motivo)
- [ ] Testes do calendário: cálculo de posições e sobreposição (unitários) + agendar/remarcar no navegador (E2E)
- [ ] Testes de concorrência de agendamento

## Fase 4 — Prontuário
- [ ] `Prontuario`, `VinculoProfissionalPaciente`, transferência
- [ ] `Procedimento` imutável + `AdendoProcedimento`
- [ ] Revisão de anamnese pelo profissional
- [ ] Acesso de emergência
- [ ] Auditoria de leitura
- [ ] Bloqueio de conteúdo clínico para ADMIN
- [ ] Telas do profissional

## Fase 5 — Financeiro
- [ ] `ConfiguracaoTaxa` e `ConfiguracaoRepasse` com vigência + seeds (CONTEXTO §8.5 e §8.6; forma do seed depende de D-048)
- [ ] Telas de configuração de taxas e repasses
- [ ] `Orcamento` + `ItemOrcamento` + desconto (% ou valor, motivo, aprovação)
- [ ] Calculadora (GARANTIR_LIQUIDO / ACRESCIMO_SIMPLES) + testes com os exemplos de CONTEXTO §8.3
- [ ] `Pagamento` com snapshot; pagamento misto
- [ ] `Parcela` com distribuição de centavos; status e acompanhamento
- [ ] Simulação exibida ao paciente (total, taxa, nº e valor das parcelas)
- [ ] Cancelamento e estorno
- [ ] `Repasse` (regra configurável — D-012/D-035 a confirmar)
- [ ] Relatórios: recebido, em aberto, atrasado, repasses por profissional

## Fase 6 — Documentos e assinaturas
- [ ] Interface `ProvedorAssinatura` + implementação interna (OTP + desenho + evidências)
- [ ] Assinatura de procedimento (profissional + paciente)
- [ ] Envio seguro por e-mail (link autenticado / PDF com senha)
- [ ] Integração com plataforma escolhida (D-016)
- [ ] ICP-Brasil para profissional

## Fase 7 — Integrações
- [ ] `GatewayPagamento` + webhooks idempotentes
- [ ] SMS / WhatsApp (`CanalNotificacao`)
- [ ] Chatbot da recepção (ações a definir — D-042)

## Fase 8 — CD e deploy (adiado — D-046)
- [ ] Definir hospedagem e backups (D-041)
- [ ] Definir storage de produção com análise de custos (volume de documentos, armazenamento, transferência, backup, hospedagem no Brasil — D-060)
- [ ] Publicar imagem Docker no GHCR em merge na `main`
- [ ] Pipeline de deploy

## Governança LGPD (fora do código)
- [ ] Procedimento de backup e rotação das chaves de criptografia (D-052) — antes de produção
- [ ] Nomear Encarregado (DPO)
- [ ] Política de privacidade e termos (revisados por advogado)
- [ ] RIPD
- [ ] Registro das operações de tratamento
- [ ] Política de retenção
- [ ] Plano de resposta a incidentes
- [ ] Contratos com operadores (hospedagem, e-mail, assinatura, gateway)
