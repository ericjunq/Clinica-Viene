# ClinicaViene — Checklist

> Atualizar a cada alteração. Legenda: `[ ]` pendente · `[~]` em andamento · `[x]` concluído
> Item concluído sem testes executados: marcar `[~]` e anotar **"testes pendentes de execução"**.
> Cada item deve indicar a branch onde foi feito.

## Fase 0 — Fundação do repositório
Branches empilhadas, mergear nesta ordem (D-054): `docs-fundacao-projeto` → `backend-estrutura-monorepo` → `docker-compose-base` → `ci-pipeline-inicial`.

- [x] Gerar projeto no Spring Initializr e subir para o GitHub (commit inicial na `main`)
- [x] Adicionar `CLAUDE.md`, `docs/`, `.claude/settings.json` ao repositório — branch `docs-fundacao-projeto`
- [x] Registrar decisões da Fase 0 (D-043, D-044, D-046, D-047, D-049 a D-055) e criar `docs/DUVIDAS.md` — branch `docs-fundacao-projeto`
- [x] Mover projeto Spring para `backend/` + `.gitignore` geral na raiz — branch `backend-estrutura-monorepo`
- [~] Spring Modulith 2.2.0 GA; remover `thymeleaf-extras-springsecurity6` (D-049, D-051) — branch `backend-estrutura-monorepo` — **implementado — testes pendentes de execução** (sem JDK nesta máquina; sem push possível — ver DUVIDAS Q-008)
- [~] Dependências manuais no `pom.xml` (Actuator, springdoc, MapStruct, openhtmltopdf, ZXing, AWS SDK S3, Bucket4j, REST Assured, ArchUnit, GreenMail) — branch `backend-estrutura-monorepo` — **implementado — testes pendentes de execução** (sem JDK nesta máquina; sem push possível — ver DUVIDAS Q-008)
  - Testcontainers MinIO **não** adicionado: imagem indisponível (D-056)
- [~] Lombok: `lombok.config`, ordem dos annotation processors, teste ArchUnit das regras (`EntidadesLombokArchTest`) (D-051) — branch `backend-estrutura-monorepo` — **implementado — testes pendentes de execução** (sem JDK nesta máquina; sem push possível — ver DUVIDAS Q-008)
- [~] Plugins: JaCoCo, Spotless, Surefire (`*Test`) / Failsafe (`*IT`) — branch `backend-estrutura-monorepo` — **implementado — testes pendentes de execução** (sem JDK nesta máquina; sem push possível — ver DUVIDAS Q-008)
  - [ ] Rodar `./mvnw spotless:apply` numa máquina com Java e ligar o `spotless:check` no build/CI (Q-004)
  - [ ] Metas de cobertura JaCoCo por módulo quando os módulos existirem
- [~] Flyway: `ddl-auto=validate`, migration `btree_gist`, tabela `event_publication` do Modulith; `db/testdata` só no perfil de teste (`MigracoesIT`) — branch `backend-estrutura-monorepo` — **implementado — testes pendentes de execução** (sem JDK nesta máquina; sem push possível — ver DUVIDAS Q-008)
- [~] Teste de módulos (`ModulosTest` — `ApplicationModules.verify()`) — branch `backend-estrutura-monorepo` — **implementado — testes pendentes de execução** (sem JDK nesta máquina; sem push possível — ver DUVIDAS Q-008)
- [ ] `compose.yaml` na raiz (Postgres 18.7-alpine, Mailpit, backend no profile `app`), `.env.example`, Dockerfile multi-stage do backend (D-050) — branch `docker-compose-base`
- [ ] Storage S3 local no compose (substituto do MinIO — D-056 A CONFIRMAR)
- [ ] CI: testes do backend (`./mvnw verify`) a cada push e em PR para `main` (D-046) — branch `ci-pipeline-inicial`
- [ ] CI: testes do frontend, lint (Spotless), cobertura (JaCoCo), scan de dependências, build de imagem Docker (conforme existirem)
- [ ] Gerar frontend Angular (`frontend/`) + Dockerfile multi-stage do frontend
- [ ] Proteção da branch `main` (PR + CI verde) — configuração no GitHub, feita pelo autor
- [x] Política de migrations registrada (D-047) — branch `docs-fundacao-projeto`
- [ ] Decidir forma de carregar seeds de configuração real (D-048 — A CONFIRMAR)

## Fase 1 — Identidade, auditoria e base LGPD
- [ ] Entidade `Usuario` + migração Flyway
- [ ] Login CPF + senha, JWT + refresh token (hash, rotação, revogação)
- [ ] 2FA para usuários internos
- [ ] Criação de usuários por DEV/MASTER; regras de perfil
- [ ] Rate limit de login
- [ ] `AuditLog` append-only + aspecto/listener de auditoria
- [ ] `TermoVersao` + `AceiteTermo`
- [ ] Problem Details padronizado + OpenAPI
- [ ] Telas: login, 2FA, gestão de usuários
- [ ] Testes de autorização por perfil

## Fase 2 — Pacientes e anamnese
- [ ] `Paciente`, `ResponsavelLegal`, histórico de versões
- [ ] Criptografia do CPF: `AttributeConverter` AES-GCM com chave versionada + `cpf_hash` HMAC-SHA256 + índice único parcial (D-052)
- [ ] Regras de CPF: opcional para menor (CPF do responsável obrigatório); adulto sem CPF só com aprovação do MASTER + auditoria; alerta de duplicidade (D-053)
- [ ] Pré-cadastro pela recepção
- [ ] `ConviteCadastro` (token hash, uso único, 72h) + QR code + e-mail
- [ ] Página pública de cadastro via link (confirmação de identidade)
- [ ] Modo quiosque (tablet)
- [ ] Cadastro pela recepção com origem `RECEPCAO`
- [ ] `ModeloAnamnese` versionado + `FichaAnamnese` + validade
- [ ] Geração de PDF + hash + storage privado
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
