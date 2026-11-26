# ClinicaViene — Checklist

> Atualizar a cada alteração. Legenda: `[ ]` pendente · `[~]` em andamento · `[x]` concluído
> Item concluído sem testes executados: marcar `[~]` e anotar **"testes pendentes de execução"**.
> Desenvolvimento direto na `main` (D-058). Itens antigos citam a branch em que foram feitos.

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
- [x] Teste de módulos (`ModulosTest` — `ApplicationModules.verify()`) — branch `backend-estrutura-monorepo` — **CI verde** (2026-11-26)
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
- [ ] Instalar PrimeNG + FullCalendar quando começarem as telas (D-043)
- [ ] **Tarefa manual do autor**: ruleset na `main` no GitHub bloqueando force push e exclusão (sem exigir PR — desenvolvimento direto na `main`, D-058/D-065)
- [x] Política de migrations registrada (D-047) — branch `docs-fundacao-projeto`
- [~] Renomear pacote para `br.com.clinicaviene` (groupId, artifactId `clinica-viene`, fixtures em `fixtures.lombok`) — D-062
- [~] Eventos do Modulith: `completion-mode=archive`, republicação ao reiniciar, `event_publication_archive` via Flyway (V3), init automático desligado — D-059
- [ ] Limpeza periódica de `event_publication_archive` (job agendado; definir prazo de retenção junto da D-040)
- [x] Pasta `docker/` dispensada: Dockerfiles em `backend/` e `frontend/`, compose na raiz (D-064)
- [ ] Decidir forma de carregar seeds de configuração real (D-048 — A CONFIRMAR)

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
- [ ] Edição de dados do usuário (nome, e-mail, telefone, registro) e mudança de perfil — não incluídas na 1C; definir regras

**1D. Autenticação**
- [~] Sessões: JWT RS256 15 min (`JWT_CHAVE_PRIVADA`), token de renovação opaco (hash SHA-256) com rotação, fim absoluto de 12 h, reuso → revoga a cadeia (tolerância de 20 s para corrida entre abas → 409), logout (`ServicoSessoesIT`)
- [ ] Login em 2 passos com desafio + endpoints `/auth/*` + cookie (D-071)
- [~] Status checado a cada requisição (`ConversorJwt`); desativar revoga sessões (D-068); troca de senha pendente → só rotas de troca de senha
- [ ] Rate limit de login (Bucket4j, IP + CPF)

**1E. 2FA**
- [ ] TOTP (RFC 6238) + QR code + 10 códigos de recuperação (D-072)
- [ ] Código 2FA por e-mail com regras de reenvio (D-073)

**1F. Frontend**
- [ ] PrimeNG; telas de login, 2FA (cadastro com QR, verificação, recuperação, envio por e-mail), troca de senha, usuários (lista, cadastro, ativar/desativar)
- [ ] Interceptor de renovação automática + guards por perfil
- [ ] Playwright + job E2E no CI (compose completo)

**1G. Recuperação de senha por e-mail** (opcional)
- [ ] Token de uso único e curta duração, com regras de reenvio (D-073)

**Testes da fase**
- [ ] Matriz de autorização (cada perfil × cada endpoint)
- [ ] Cobertura JaCoCo ≥ 80% em `identidade`

## Fase 2 — Pacientes e anamnese
- [ ] `TermoVersao` + `AceiteTermo` (movidos da Fase 1 — D-075)
- [ ] Anamnese, prontuário e categorias por área de atuação (D-070 — A CONFIRMAR)
- [ ] `Paciente`, `ResponsavelLegal`, histórico de versões
- [ ] Criptografia do CPF: `AttributeConverter` AES-GCM com chave versionada + `cpf_hash` HMAC-SHA256 + índice único parcial (D-052) — se a Fase 1 vier antes, implementar já no `Usuario` (D-061)
- [ ] Regras de CPF: opcional para menor (CPF do responsável obrigatório); adulto sem CPF só com aprovação do MASTER + auditoria; alerta de duplicidade (D-053)
- [ ] Pré-cadastro pela recepção
- [ ] `ConviteCadastro` (token hash, uso único, 72h) + QR code + e-mail
- [ ] Página pública de cadastro via link (confirmação de identidade)
- [ ] Modo quiosque (tablet)
- [ ] Cadastro pela recepção com origem `RECEPCAO`
- [ ] `ModeloAnamnese` versionado + `FichaAnamnese` + validade
- [ ] Geração de PDF + hash + storage privado (AWS SDK S3 com endpoint configurável; testes com SeaweedFS em `GenericContainer` — D-056)
- [ ] Testes de fluxo completo (E2E)

## Fase 3 — Agenda
- [ ] `Consultorio`
- [ ] `DisponibilidadeProfissional` (recorrente) + `ExcecaoAgenda` + `BloqueioAgenda`
- [ ] `Agendamento` com status completo + exclusão de conflitos no banco
- [ ] Marcar / remarcar / cancelar (com motivo)
- [ ] Check-in + notificação WebSocket ao profissional
- [ ] Telas: agenda por profissional/consultório (FullCalendar — D-043), check-in
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
